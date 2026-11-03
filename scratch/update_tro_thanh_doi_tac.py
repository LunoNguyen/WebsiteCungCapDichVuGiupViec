import sys
sys.stdout.reconfigure(encoding='utf-8')

html_path = r'd:\2026\HK1_Nam4\LuanAn\SourceCode\WebsiteCungCapDichVuGiupViec\src\main\resources\templates\site\tro-thanh-doi-tac.html'
with open(html_path, 'r', encoding='utf-8') as f:
    html = f.read()

# Update submit button
old_btn = '<button class="btn btn-teal is-locked" type="submit" id="ctvSubmit" disabled style="opacity:0.5">Gửi hồ sơ</button>'
new_btn = '<button class="btn btn-teal" type="submit" id="ctvSubmit">Gửi hồ sơ</button>'

if old_btn in html:
    html = html.replace(old_btn, new_btn)
    print('Updated ctvSubmit button in tro-thanh-doi-tac.html')
else:
    print('Warning: old_btn not found')

# Update script tag with cache-busting timestamp
old_script = '<script src="/js/dang-ky-ctv.js"></script>'
new_script = '<script src="/js/dang-ky-ctv.js?v=20261104_02"></script>'

if old_script in html:
    html = html.replace(old_script, new_script)
    print('Updated script tag with cache buster in tro-thanh-doi-tac.html')
else:
    print('Warning: old_script not found')

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(html)
print('HTML update complete.')

