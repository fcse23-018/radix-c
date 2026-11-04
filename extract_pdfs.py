from pypdf import PdfReader
import glob
import os

for p in glob.glob('*.pdf'):
    reader = PdfReader(p)
    out = []
    for i, page in enumerate(reader.pages):
        out.append("\n\n===== PAGE %d =====\n" % (i + 1))
        out.append(page.extract_text() or "")
    name = 'extracted_' + p.replace(' ', '_').replace('&', 'and') + '.txt'
    with open(name, 'w', encoding='utf-8') as f:
        f.write(''.join(out))
    print(p, len(reader.pages), os.path.getsize(name))
