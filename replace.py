import os
import glob

directory = 'd:/AndroidStudioProjects/BackToYou/app/src/main/java'
files = glob.glob(directory + '/**/*.java', recursive=True)

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    if '"idlf26"' in content:
        new_content = content.replace('"idlf26"', '"lf26"')
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file_path}')
