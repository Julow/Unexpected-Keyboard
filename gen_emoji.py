import urllib.request
import os.path

EMOJIS_PATH = 'res/raw/emojis.txt'
EMOJI_TEST_PATH = 'emoji-test.txt'
EMOJI_TEST_URL = 'https://unicode.org/Public/emoji/latest/emoji-test.txt'

def rawEmojiFromCodes(codes):
    return ''.join([chr(int(c, 16)) for c in codes])

def getEmojiTestContents():
    if os.path.exists(EMOJI_TEST_PATH):
        print(f'Using existing {EMOJI_TEST_PATH}')
    else:
        print(f'Downloading {EMOJI_TEST_URL}')
        urllib.request.urlretrieve(EMOJI_TEST_URL, EMOJI_TEST_PATH)
    return open(EMOJI_TEST_PATH, mode='r', encoding='UTF-8').read()
        

emoji_list = []
group_indices = []
for line in getEmojiTestContents().splitlines():
    if line.startswith('# group:'):
        if len(group_indices) == 0 or len(emoji_list) > group_indices[-1]:
            group_indices.append(len(emoji_list))
    elif not line.startswith('#') and 'fully-qualified' in line:
        codes = line.split(';')[0].split()
        emoji_list.append(rawEmojiFromCodes(codes))

with open(EMOJIS_PATH, 'w', encoding='UTF-8') as emojis:
    for e in emoji_list:
        emojis.write(f'{e}\n')
    emojis.write('\n')
    
    for g in group_indices:
        emojis.write(f'{g}\n')

print(f'Parsed {len(emoji_list)} emojis in {len(group_indices)}')
