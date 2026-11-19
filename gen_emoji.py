import urllib.request
import os.path

EMOJIS_PATH = 'res/raw/emojis.txt'
EMOJI_TEST_PATH = 'emoji-test.txt'
EMOJI_TEST_URL = 'https://unicode.org/Public/emoji/latest/emoji-test.txt'
SKIN_TONES = [chr(c) for c in range(0x1F3FB, 0x1F400)]

def rawEmojiFromCodes(codes):
    return ''.join([chr(int(c, 16)) for c in codes])

def getEmojiTestContents():
    if os.path.exists(EMOJI_TEST_PATH):
        print(f'Using existing {EMOJI_TEST_PATH}')
    else:
        print(f'Downloading {EMOJI_TEST_URL}')
        urllib.request.urlretrieve(EMOJI_TEST_URL, EMOJI_TEST_PATH)
    return open(EMOJI_TEST_PATH, mode='r', encoding='UTF-8').read()

# "kiss: woman, man, light skin tone" -> "kiss: woman, man"
def baseName(name):
    base, _, qualifiers = name.partition(': ')
    kept = [q for q in qualifiers.split(', ') if q and not q.endswith('skin tone')]
    return f'{base}: {", ".join(kept)}' if kept else base

def baseEmoji(name):
    base = baseName(name)
    # "kiss: person, person, light skin tone, dark skin tone" is a "kiss"
    return emoji_by_name.get(base) or emoji_by_name[base.partition(':')[0]]

def isUniformTone(emoji, tone):
    return all(c == tone for c in emoji if c in SKIN_TONES)

# Variants of an emoji: a line per base emoji, followed by its skin tone
# variants. The first five variants are the uniform tones, in order.
variants = {}
emoji_by_name = {}
group_indices = []
for line in getEmojiTestContents().splitlines():
    if line.startswith('# group:'):
        if len(group_indices) == 0 or len(variants) > group_indices[-1]:
            group_indices.append(len(variants))
    elif not line.startswith('#') and 'fully-qualified' in line:
        codes = line.split(';')[0].split()
        emoji = rawEmojiFromCodes(codes)
        name = line.split('#', 1)[1].split(' ', 3)[3]
        if any(c in SKIN_TONES for c in emoji):
            variants[baseEmoji(name)].append(emoji)
        else:
            emoji_by_name[name] = emoji
            variants[emoji] = []

for base, vs in variants.items():
    vs.sort(key=lambda v: not any(isUniformTone(v, t) for t in SKIN_TONES))
    assert len(vs) == 0 or (len(vs) >= len(SKIN_TONES)
            and all(isUniformTone(v, t) for v, t in zip(vs, SKIN_TONES))), base

with open(EMOJIS_PATH, 'w', encoding='UTF-8') as emojis:
    for base, vs in variants.items():
        emojis.write(' '.join([base] + vs) + '\n')
    emojis.write('\n')

    emojis.write(' '.join([str(g) for g in group_indices]))
    emojis.write('\n')

print(f'Parsed {len(variants)} emojis in {len(group_indices)} groups')
