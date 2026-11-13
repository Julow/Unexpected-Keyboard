#!/usr/bin/env python3
"""Generate the themed-icon (monochrome) launcher drawable.

Reads res/drawable/ic_launcher_foreground.xml and derives
res/drawable/ic_launcher_monochrome.xml from it:

  - The dark key bodies become the solid, opaque part of the icon.
  - The white glyph strokes (letters and sub-labels) are knocked out of
    the bodies, exactly like they are drawn on top of them in the
    original icon.

The monochrome drawable is a single path with the even-odd fill rule:
body subpaths are copied verbatim from the foreground (keeping the arc
commands), glyph strokes are converted to outline polygons with shapely
(round caps and joins, like the strokes they replace) and appended as
subpaths.  The system launcher tint the alpha channel of this drawable
on Android 13+.

Usage: python gen_monochrome.py
Also writes preview images to build/monochrome_preview/.
"""

import math
import os
import re
import xml.etree.ElementTree as ET

from PIL import Image, ImageChops, ImageDraw
from shapely.geometry import LineString, LinearRing, Polygon
from shapely.ops import unary_union

ROOT = os.path.dirname(os.path.abspath(__file__))
FOREGROUND = os.path.join(ROOT, 'res', 'drawable', 'ic_launcher_foreground.xml')
ADAPTIVE_ICON = os.path.join(ROOT, 'res', 'mipmap-anydpi-v26', 'ic_launcher.xml')
OUTPUT = os.path.join(ROOT, 'res', 'drawable', 'ic_launcher_monochrome.xml')
PREVIEW_DIR = os.path.join(ROOT, 'build', 'monochrome_preview')

VIEWPORT = 1100.0
# Resolution of the curve flattening and of the shapely buffers, in
# fraction of a circle quadrant.  The rounding error stays far below a
# pixel at the largest rendered icon size (432px).
CURVE_SEGMENTS = 48
BUFFER_QUAD_SEGS = 64

ANDROID_NS = 'http://schemas.android.com/apk/res/android'


# ---------------------------------------------------------------------------
# SVG path parsing

TOKEN_RE = re.compile(r'[MmLlHhVvCcSsQqTtAaZz]|-?(?:\d+\.?\d*|\.\d+)')


def _arc_to_points(p0, rx, ry, phi_deg, large_arc, sweep, p1):
  """Sample an elliptical arc (endpoint parameterization, SVG semantics)."""
  phi = math.radians(phi_deg)
  x1, y1 = p0
  x2, y2 = p1
  dx, dy = (x1 - x2) / 2.0, (y1 - y2) / 2.0
  c, s = math.cos(phi), math.sin(phi)
  x1p, y1p = c * dx + s * dy, -s * dx + c * dy
  rx, ry = abs(rx), abs(ry)
  if rx == 0 or ry == 0:
    return [p1]
  lam = x1p ** 2 / rx ** 2 + y1p ** 2 / ry ** 2
  if lam > 1:
    k = math.sqrt(lam)
    rx, ry = rx * k, ry * k
  sign = 1.0 if large_arc != sweep else -1.0
  num = rx ** 2 * ry ** 2 - rx ** 2 * y1p ** 2 - ry ** 2 * x1p ** 2
  den = rx ** 2 * y1p ** 2 + ry ** 2 * x1p ** 2
  co = sign * math.sqrt(max(0.0, num / den))
  cxp, cyp = co * rx * y1p / ry, -co * ry * x1p / rx
  cx = c * cxp - s * cyp + (x1 + x2) / 2.0
  cy = s * cxp + c * cyp + (y1 + y2) / 2.0
  def angle(ux, uy, vx, vy):
    dot = ux * vx + uy * vy
    n = math.hypot(ux, uy) * math.hypot(vx, vy)
    a = math.acos(max(-1.0, min(1.0, dot / n)))
    return -a if ux * vy - uy * vx < 0 else a
  u = (x1p - cxp) / rx, (y1p - cyp) / ry
  v = (-x1p - cxp) / rx, (-y1p - cyp) / ry
  dtheta = angle(*u, *v)
  if not sweep and dtheta > 0:
    dtheta -= 2 * math.pi
  elif sweep and dtheta < 0:
    dtheta += 2 * math.pi
  n = max(2, int(math.ceil(abs(dtheta) / (math.pi / 2 / CURVE_SEGMENTS)) - 1e-9))
  theta1 = math.atan2(u[1], u[0])
  pts = []
  for i in range(1, n + 1):
    t = theta1 + dtheta * i / n
    px = cx + rx * c * math.cos(t) - ry * s * math.sin(t)
    py = cy + rx * s * math.cos(t) + ry * c * math.sin(t)
    pts.append((px, py))
  return pts


def _cubic_points(p0, p1, p2, p3):
  pts = []
  for i in range(1, CURVE_SEGMENTS + 1):
    t = i / CURVE_SEGMENTS
    mt = 1.0 - t
    x = mt**3 * p0[0] + 3 * mt**2 * t * p1[0] + 3 * mt * t**2 * p2[0] + t**3 * p3[0]
    y = mt**3 * p0[1] + 3 * mt**2 * t * p1[1] + 3 * mt * t**2 * p2[1] + t**3 * p3[1]
    pts.append((x, y))
  return pts


def _quad_points(p0, p1, p2):
  pts = []
  for i in range(1, CURVE_SEGMENTS + 1):
    t = i / CURVE_SEGMENTS
    mt = 1.0 - t
    x = mt**2 * p0[0] + 2 * mt * t * p1[0] + t**2 * p2[0]
    y = mt**2 * p0[1] + 2 * mt * t * p1[1] + t**2 * p2[1]
    pts.append((x, y))
  return pts


def parse_path(d):
  """Parse a path string into a list of subpaths [(points, closed)]."""
  tokens = TOKEN_RE.findall(d)
  subs = []
  cur = []
  start = (0.0, 0.0)
  pos = (0.0, 0.0)
  prev_cubic_cp = None
  prev_quad_cp = None
  prev_cmd = ''
  closed = False
  i = 0

  def flush():
    nonlocal cur, closed
    if len(cur) >= 2:
      subs.append((cur, closed))
    cur = []
    closed = False

  def need(cmd, n):
    nonlocal i
    vals = [float(tokens[i + k]) for k in range(n)]
    i += n
    return vals

  while i < len(tokens):
    t = tokens[i]
    if re.match(r'[A-Za-z]', t):
      cmd = t
      i += 1
      relative = cmd.islower()
      cmd = cmd.upper()
    else:
      # Repeat the previous command with new parameters.
      pass
    if cmd == 'M':
      while True:
        x, y = need('M', 2)
        if relative:
          x += pos[0]
          y += pos[1]
        flush()
        cur = [(x, y)]
        pos = (x, y)
        start = (x, y)
        cmd = 'L' if not relative else 'l'
        relative = False
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd == 'L':
      while True:
        x, y = need('L', 2)
        if relative:
          x += pos[0]
          y += pos[1]
        cur.append((x, y))
        pos = (x, y)
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd == 'H':
      while True:
        (x,) = need('H', 1)
        if relative:
          x += pos[0]
        cur.append((x, pos[1]))
        pos = (x, pos[1])
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd == 'V':
      while True:
        (y,) = need('V', 1)
        if relative:
          y += pos[1]
        cur.append((pos[0], y))
        pos = (pos[0], y)
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd in ('C', 'S'):
      while True:
        if cmd == 'C':
          x1, y1, x2, y2, x, y = need('C', 6)
          if relative:
            x1, y1, x2, y2, x, y = (x1 + pos[0], y1 + pos[1], x2 + pos[0],
                                    y2 + pos[1], x + pos[0], y + pos[1])
          prev_cubic_cp = (x2, y2)
        else:
          x1, y1 = (2 * pos[0] - prev_cubic_cp[0], 2 * pos[1] - prev_cubic_cp[1]) \
              if prev_cmd in ('C', 'S') else (pos[0], pos[1])
          x2, y2, x, y = need('S', 4)
          if relative:
            x2, y2, x, y = x2 + pos[0], y2 + pos[1], x + pos[0], y + pos[1]
          prev_cubic_cp = (x2, y2)
        cur.extend(_cubic_points(pos, (x1, y1), (x2, y2), (x, y)))
        pos = (x, y)
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd in ('Q', 'T'):
      while True:
        if cmd == 'Q':
          x1, y1, x, y = need('Q', 4)
          if relative:
            x1, y1, x, y = x1 + pos[0], y1 + pos[1], x + pos[0], y + pos[1]
          prev_quad_cp = (x1, y1)
        else:
          x1, y1 = (2 * pos[0] - prev_quad_cp[0], 2 * pos[1] - prev_quad_cp[1]) \
              if prev_cmd in ('Q', 'T') else (pos[0], pos[1])
          x, y = need('T', 2)
          if relative:
            x, y = x + pos[0], y + pos[1]
          prev_quad_cp = (x1, y1)
        cur.extend(_quad_points(pos, (x1, y1), (x, y)))
        pos = (x, y)
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd == 'A':
      while True:
        rx, ry, rot, laf, sf, x, y = need('A', 7)
        if relative:
          x, y = x + pos[0], y + pos[1]
        cur.extend(_arc_to_points(pos, rx, ry, rot, laf, sf, (x, y)))
        pos = (x, y)
        if i >= len(tokens) or re.match(r'[A-Za-z]', tokens[i]):
          break
    elif cmd == 'Z':
      if cur:
        cur.append(cur[0])
      closed = True
      pos = start
      prev_cubic_cp = prev_quad_cp = None
      flush()
    else:
      raise ValueError('unsupported command: %s' % cmd)
    prev_cmd = cmd

  flush()
  return subs


# ---------------------------------------------------------------------------
# Geometry

def stroke_to_polygon(points, closed, half_width, cap):
  """Outline of a stroke centered on `points`, as a shapely geometry."""
  line = LinearRing(points) if closed else LineString(points)
  cap_style = {'round': 'round', 'square': 'square', 'flat': 'flat'}[cap]
  return line.buffer(half_width, cap_style=cap_style, join_style='round',
                     quad_segs=BUFFER_QUAD_SEGS)


def subpaths_to_polygon(subs):
  """Union of the polygons of flattened, closed subpaths (non-zero rule)."""
  return unary_union([Polygon(pts) for pts, closed in subs if closed])


def geometry_to_rings(geom):
  polys = geom.geoms if hasattr(geom, 'geoms') else [geom]
  rings = []
  for poly in polys:
    rings.append(list(poly.exterior.coords))
    rings.extend(list(h.coords) for h in poly.interiors)
  return rings


def detect_circle(pts):
  """Return (cx, cy, r) if the flattened ring is a circle, else None."""
  xs = [p[0] for p in pts[:-1]]
  ys = [p[1] for p in pts[:-1]]
  cx, cy = sum(xs) / len(xs), sum(ys) / len(ys)
  radii = [math.hypot(x - cx, y - cy) for x, y in zip(xs, ys)]
  r = sum(radii) / len(radii)
  if max(abs(v - r) for v in radii) < 0.05:
    return (cx, cy, r)
  return None


def fmt(v):
  s = '%.2f' % v
  s = s.rstrip('0').rstrip('.')
  return '0' if s in ('-0', '') else s


def ring_path_data(pts):
  if len(pts) > 4 and pts[0] == pts[-1]:
    circle = detect_circle(pts)
    if circle is not None:
      cx, cy, r = circle
      return ('M%s,%sA%s,%s 0 1 0 %s,%sA%s,%s 0 1 0 %s,%sZ'
              % (fmt(cx + r), fmt(cy), fmt(r), fmt(r),
                 fmt(cx - r), fmt(cy), fmt(r), fmt(r),
                 fmt(cx + r), fmt(cy)))
  pts = pts[:-1] if pts[0] == pts[-1] else pts
  return 'M' + 'L'.join('%s,%s' % (fmt(x), fmt(y)) for x, y in pts) + 'Z'


# ---------------------------------------------------------------------------
# Generation

def main():
  tree = ET.parse(FOREGROUND)
  paths = tree.getroot().findall('path')

  bodies = []   # (subpaths, original path data)
  strokes = []  # (subpaths, half width, cap style)
  for p in paths:
    d = p.get('{%s}pathData' % ANDROID_NS)
    fill = p.get('{%s}fillColor' % ANDROID_NS, '')
    stroke = p.get('{%s}strokeColor' % ANDROID_NS, '')
    subs = parse_path(d)
    if stroke:
      width = float(p.get('{%s}strokeWidth' % ANDROID_NS))
      cap = p.get('{%s}strokeLineCap' % ANDROID_NS, 'round')
      strokes.append((subs, width / 2.0, cap))
    elif fill and fill.lower() not in ('#00ffffff', '#00000000'):
      bodies.append((subs, d))

  # Key bodies: deduplicate repeated subpaths, keep the original data.
  body_parts = []       # flattened subpaths, for geometry and previews
  body_data_parts = []  # original path data, verbatim
  seen = set()
  for subs, d in bodies:
    raw_subs = re.findall(r'[Mm][^Mm]+', d)
    flat = [(pts, closed) for pts, closed in subs if closed]
    if len(raw_subs) != len(flat):
      raise RuntimeError('unexpected body path structure')
    for raw, (pts, _closed) in zip(raw_subs, flat):
      key = tuple((round(x, 2), round(y, 2)) for x, y in pts)
      if key in seen:
        continue
      seen.add(key)
      body_parts.append((pts, _closed))
      body_data_parts.append(raw.strip())

  body_geom = subpaths_to_polygon(body_parts)

  # Glyph strokes: outline them and merge overlapping ones (the tail of
  # the "9" crosses its bowl, the stem of the "p" meets its bowl).
  glyph_parts = []
  for subs, half_width, cap in strokes:
    for pts, closed in subs:
      if closed and pts[0] == pts[-1]:
        pts = pts[:-1]
      glyph_parts.append(stroke_to_polygon(pts, closed, half_width, cap))
  glyphs_geom = unary_union(glyph_parts)

  # The strokes are drawn on top of the keys in the original icon and
  # may poke slightly past their edges (the comma of the top-right key
  # does, by less than a unit).  Clip the holes to the body shrunk by a
  # small margin so they always sit strictly inside a key; the clipped
  # tip stays within a fraction of a pixel at render size.
  HOLE_MARGIN = 0.6
  holes_geom = glyphs_geom.intersection(body_geom.buffer(-HOLE_MARGIN))

  hole_data = ''.join(ring_path_data(ring)
                      for ring in geometry_to_rings(holes_geom))

  # One <path> per key body: the body and the glyph holes that sit
  # inside it share the even-odd parity of that path.  Splitting the
  # data also keeps every pathData string well below the ~32KB limit
  # of the AAPT2 string pool, beyond which it silently replaces the
  # value with "STRING_TOO_LARGE" in the binary XML.
  body_polys = [Polygon(pts) for pts, _closed in body_parts]
  hole_polys = (holes_geom.geoms if hasattr(holes_geom, 'geoms')
                else [holes_geom])
  chunks = []
  assigned = 0
  for raw, poly in zip(body_data_parts, body_polys):
    holes_here = [h for h in hole_polys if poly.contains(h.representative_point())]
    assigned += len(holes_here)
    data = raw + ''.join(ring_path_data(r)
                         for hp in holes_here
                         for r in geometry_to_rings(hp))
    if len(data) > 30000:
      raise RuntimeError('pathData too long for the AAPT2 string pool')
    chunks.append(data)
  if assigned != len(hole_polys):
    raise RuntimeError('some glyph holes are not inside any key body')

  path_lines = '\n'.join(
    '  <path android:fillColor="#ffffffff" android:fillType="evenOdd"'
    ' android:pathData="%s"/>' % data for data in chunks)
  xml = (
    '<?xml version="1.0" encoding="utf-8"?>\n'
    '<!-- Generated by gen_monochrome.py, do not edit. -->\n'
    '<vector xmlns:android="http://schemas.android.com/apk/res/android"'
    ' android:height="108dp" android:width="108dp"'
    ' android:viewportWidth="%s" android:viewportHeight="%s">\n'
    '%s\n'
    '</vector>\n' % (fmt(VIEWPORT), fmt(VIEWPORT), path_lines))

  with open(OUTPUT, 'w', newline='\n') as f:
    f.write(xml)
  print('wrote %s (%d paths, %d rings, %d chars of path data)'
        % (os.path.relpath(OUTPUT, ROOT), len(chunks),
           len(geometry_to_rings(holes_geom)), len(hole_data)))

  render_previews(body_parts, holes_geom)


# ---------------------------------------------------------------------------
# Preview rendering

SS = 4  # supersampling

def _mask_from_rings(rings, xor=False):
  """Render filled rings into a 'L' mask; XOR-accumulate when nested."""
  img = Image.new('L', (int(VIEWPORT * SS), int(VIEWPORT * SS)), 0)
  draw = ImageDraw.Draw(img)
  if not xor:
    for ring in rings:
      draw.polygon([(x * SS, y * SS) for x, y in ring], fill=255)
    return img
  for ring in rings:
    tmp = Image.new('L', img.size, 0)
    ImageDraw.Draw(tmp).polygon([(x * SS, y * SS) for x, y in ring], fill=255)
    img = ImageChops.difference(img, tmp)
  return img


def _composite(mask, fg, bg):
  icon = bg.copy() if isinstance(bg, Image.Image) else \
      Image.new('RGB', mask.size, bg)
  layer = Image.new('RGB', mask.size, fg)
  icon.paste(layer, (0, 0), mask)
  return icon


def render_previews(body_parts, holes_geom):
  os.makedirs(PREVIEW_DIR, exist_ok=True)
  dark = (16, 16, 16)

  # The original icon: key bodies with the glyphs stroked on top.
  body_mask = _mask_from_rings([pts for pts, _ in body_parts])
  stroke_band = _mask_from_rings(geometry_to_rings(holes_geom), xor=True)
  original = _composite(body_mask, (42, 42, 42), dark)
  original = _composite(stroke_band, (255, 255, 255), original)
  original = original.resize((432, 432), Image.LANCZOS)
  original.save(os.path.join(PREVIEW_DIR, 'original.png'))

  # The generated monochrome drawable, parsed back from the output file.
  mono_tree = ET.parse(OUTPUT)
  rings = []
  for p in mono_tree.getroot().findall('path'):
    d = p.get('{%s}pathData' % ANDROID_NS)
    rings.extend(pts for pts, closed in parse_path(d) if closed)
  mono_mask = _mask_from_rings(rings, xor=True)
  mono = _composite(mono_mask, (230, 230, 230), dark)
  mono = mono.resize((432, 432), Image.LANCZOS)
  mono.save(os.path.join(PREVIEW_DIR, 'monochrome.png'))

  # Simulated themed icons (Material You tints over a dark backdrop).
  tints = [(214, 218, 236), (255, 214, 160), (170, 220, 180), (240, 170, 190)]
  strip = Image.new('RGB', (4 * 200 + 5 * 16, 200 + 32), dark)
  for i, tint in enumerate(tints):
    chip = _composite(mono_mask, tint, dark).resize((200, 200), Image.LANCZOS)
    strip.paste(chip, (16 + i * (200 + 16), 16))
  strip.save(os.path.join(PREVIEW_DIR, 'themed_sim.png'))

  # Side by side comparison.
  side = Image.new('RGB', (432 * 2 + 48, 432 + 32), (40, 40, 40))
  side.paste(original, (16, 16))
  side.paste(mono, (432 + 32, 16))
  side.save(os.path.join(PREVIEW_DIR, 'side_by_side.png'))
  print('previews written to %s' % os.path.relpath(PREVIEW_DIR, ROOT))


if __name__ == '__main__':
  main()
