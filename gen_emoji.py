import re
import urllib.request
import os.path

EMOJIS_PATH = 'res/raw/emojis.txt'
EMOJI_TEST_PATH = 'emoji-test.txt'
EMOJI_TEST_URL = 'https://unicode.org/Public/emoji/latest/emoji-test.txt'
SKIN_TONES = [chr(c) for c in range(0x1F3FB, 0x1F400)]
MALE, FEMALE = 1, 2

# Gendered emojis whose non-specific version can't be derived from their name.
IRREGULAR_GENDERS = {
    'boy': ('child', MALE),
    'girl': ('child', FEMALE),
    'old man': ('older person', MALE),
    'old woman': ('older person', FEMALE),
    'prince': ('person with crown', MALE),
    'princess': ('person with crown', FEMALE),
    'merman': ('merperson', MALE),
    'mermaid': ('merperson', FEMALE),
    'Santa Claus': ('Mx Claus', MALE),
    'Mrs. Claus': ('Mx Claus', FEMALE),
    'woman and man holding hands': ('people holding hands', None),
}
# Groups with variants of several people, not affected by the default gender.
MULTI_PERSON = {'kiss', 'couple with heart', 'family', 'people holding hands'}

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
def skinToneBaseName(name):
    base, _, qualifiers = name.partition(': ')
    kept = [q for q in qualifiers.split(', ') if q and not q.endswith('skin tone')]
    return f'{base}: {", ".join(kept)}' if kept else base

def skinToneBase(name):
    base = skinToneBaseName(name)
    # "kiss: person, person, light skin tone, dark skin tone" is a "kiss"
    return emoji_by_name.get(base) or emoji_by_name[base.partition(':')[0]]

# A single skin tone, a tuple of skin tones for people with different skin
# tones or None for no skin tone.
def skinToneKey(emoji):
    tones = tuple(c for c in emoji if c in SKIN_TONES)
    if len(set(tones)) == 1:
        return tones[0]
    return tones or None

# "man running" -> ("person running", MALE)
def genderBase(name):
    if name in IRREGULAR_GENDERS:
        return IRREGULAR_GENDERS[name]
    prefix = name.partition(':')[0]
    if prefix in MULTI_PERSON and prefix != name:
        return prefix, None
    gender = FEMALE if re.search(r'\bwom[ae]n\b', name) else MALE
    neutral = re.sub(r'\b(wo)?m([ae])n\b',
            lambda m: 'person' if m[2] == 'a' else 'people', name)
    for candidate in (neutral, re.sub(r'^(wo)?man ', '', name)):
        if candidate != name and candidate in emoji_by_name:
            return candidate, gender
    return None

# Emojis without skin tone, in order, with their name and group number.
bases = []
emoji_by_name = {}
skin_tones = {}
group = -1
for line in getEmojiTestContents().splitlines():
    if line.startswith('# group:'):
        group += 1
    elif not line.startswith('#') and 'fully-qualified' in line:
        codes = line.split(';')[0].split()
        emoji = rawEmojiFromCodes(codes)
        name = line.split('#', 1)[1].split(' ', 3)[3]
        if any(c in SKIN_TONES for c in emoji):
            skin_tones[skinToneBase(name)][skinToneKey(emoji)] = emoji
        else:
            emoji_by_name[name] = emoji
            skin_tones[emoji] = {None: emoji}
            bases.append((emoji, name, group))

# Gendered versions of the non-specific emojis.
genders = {}
gendered = set()
for emoji, name, _ in bases:
    base = genderBase(name)
    if base is not None:
        genders.setdefault(emoji_by_name[base[0]], []).append((emoji, base[1]))
        gendered.add(emoji)

# A line per emoji. Its variants are organized in rows separated by spaces, one
# row for each skin tone: no skin tone, the 5 uniform skin tones, then mixed
# skin tones. A row contains the gender variants, separated by commas:
# non-specific, male and female for single persons, or the non-specific
# version followed by the others in Unicode order for groups of people.
lines = []
group_indices = []
last_group = None
for emoji, name, group in bases:
    if group != last_group:
        group_indices.append(len(lines))
        last_group = group
    if emoji in gendered:
        continue
    members = [(emoji, None)] + genders.get(emoji, [])
    if name in MULTI_PERSON:
        # Rows of 3 are read as non-specific, male and female.
        assert len(members) != 3, name
    else:
        members.sort(key=lambda m: m[1] or 0)
        assert [g for _, g in members] in ([None], [None, MALE, FEMALE]), members
    mixed = sorted(k for k in skin_tones[emoji] if type(k) is tuple)
    keys = [None] + (SKIN_TONES + mixed if len(skin_tones[emoji]) > 1 else [])
    for m, _ in members:
        assert set(skin_tones[m]) == set(keys), (emoji, m)
    lines.append(' '.join(','.join(skin_tones[m][k] for m, _ in members) for k in keys))

with open(EMOJIS_PATH, 'w', encoding='UTF-8') as emojis:
    for line in lines:
        emojis.write(line + '\n')
    emojis.write('\n')

    emojis.write(' '.join([str(g) for g in group_indices]))
    emojis.write('\n')

print(f'Parsed {len(lines)} emojis in {len(group_indices)} groups')
