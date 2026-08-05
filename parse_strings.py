import re
import os

def parse():
    with open('app/src/main/java/com/example/util/LocalizationManager.kt', 'r') as f:
        content = f.read()

    # Get default strings (English) from data class AppStrings(
    default_match = re.search(r'data class AppStrings\((.*?)\)', content, re.DOTALL)
    defaults = {}
    if default_match:
        lines = default_match.group(1).strip().split('\n')
        for line in lines:
            line = line.strip()
            if not line: continue
            if line.startswith('val '):
                # val key: String = "value",
                match = re.search(r'val\s+(\w+)\s*:\s*String\s*=\s*"(.*?)"', line)
                if match:
                    defaults[match.group(1)] = match.group(2)

    # Get translations for each language
    lang_blocks = re.finditer(r'"(\w+)"\s*->\s*AppStrings\((.*?)\)', content, re.DOTALL)
    
    translations = {}
    for block in lang_blocks:
        lang_code = block.group(1)
        lang_content = block.group(2)
        
        lang_strings = {}
        # match key = "value"
        matches = re.finditer(r'(\w+)\s*=\s*"(.*?)"', lang_content)
        for m in matches:
            lang_strings[m.group(1)] = m.group(2)
            
        translations[lang_code] = lang_strings

    # Helper to write strings.xml
    def write_strings(lang_code, strings_dict):
        dir_name = 'app/src/main/res/values'
        if lang_code != 'en':
            dir_name = f'app/src/main/res/values-{lang_code}'
            # specific fix for zh
            if lang_code == 'zh':
                dir_name = 'app/src/main/res/values-zh-rCN'
            if lang_code == 'pt':
                dir_name = 'app/src/main/res/values-pt'
                
        os.makedirs(dir_name, exist_ok=True)
        
        with open(os.path.join(dir_name, 'strings.xml'), 'w') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.write('<resources>\n')
            for key, val in strings_dict.items():
                # Escape XML special chars
                val = val.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace("'", "\\'").replace('"', '\\"')
                f.write(f'    <string name="{key}">{val}</string>\n')
            f.write('</resources>\n')
            
    # Write English (default)
    write_strings('en', defaults)
    
    # Write other languages
    for lang_code, strings_dict in translations.items():
        # merge with defaults if missing
        merged = defaults.copy()
        merged.update(strings_dict)
        write_strings(lang_code, merged)

if __name__ == "__main__":
    parse()
