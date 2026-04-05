import os

base_dir = r'd:\\BackToYou\\app\\src\\main'
res_dir = os.path.join(base_dir, 'res')
drawable_dir = os.path.join(res_dir, 'drawable')

os.makedirs(drawable_dir, exist_ok=True)

drawables = {
    'bg_chip_selected.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorChipSelected\"/>
    <corners android:radius=\"15dp\"/>
</shape>''',
    'bg_chip_unselected.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorChipUnselected\"/>
    <corners android:radius=\"15dp\"/>
</shape>''',
    'bg_badge_lost.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorLostBg\"/>
    <corners android:radius=\"6dp\"/>
</shape>''',
    'bg_badge_found.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorFoundBg\"/>
    <corners android:radius=\"6dp\"/>
</shape>''',
    'bg_badge_claimed.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorClaimedBg\"/>
    <corners android:radius=\"6dp\"/>
</shape>''',
    'bg_dept_tag.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorMPSTMEBg\"/>
    <corners android:radius=\"5dp\"/>
</shape>''',
    'bg_search_bar.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"@color/colorBackground\"/>
    <stroke android:width=\"1dp\" android:color=\"@color/colorBorder\"/>
    <corners android:radius=\"18dp\"/>
</shape>''',
    'bg_fab_teal.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"oval\">
    <solid android:color=\"#1DB8AB\"/>
</shape>''',
    'bg_icon_blue.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#E6F1FB\"/>
    <corners android:radius=\"10dp\"/>
</shape>''',
    'bg_icon_green.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#E8F5E9\"/>
    <corners android:radius=\"10dp\"/>
</shape>''',
    'bg_icon_amber.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#FAEEDA\"/>
    <corners android:radius=\"10dp\"/>
</shape>''',
    'bg_icon_coral.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#FAECE7\"/>
    <corners android:radius=\"10dp\"/>
</shape>''',
    'bg_icon_gray.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#F1EFE8\"/>
    <corners android:radius=\"10dp\"/>
</shape>''',
    'bg_icon_teal.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#CCFBF1\"/>
    <corners android:radius=\"10dp\"/>
</shape>''',
    'bg_icon_purple.xml': '''<?xml version=\"1.0\" encoding=\"utf-8\"?>
<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">
    <solid android:color=\"#EEEDFE\"/>
    <corners android:radius=\"10dp\"/>
</shape>'''
}

vectors = [
    'ic_add.xml', 'ic_search.xml', 'ic_empty_state.xml', 'ic_category_electronics.xml',
    'ic_category_keys.xml', 'ic_category_lab.xml', 'ic_category_wallet.xml', 'ic_category_bag.xml',
    'ic_category_bottle.xml', 'ic_category_book.xml', 'ic_category_id.xml', 'ic_category_other.xml'
]

vector_template = '''<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"
    android:width=\"24dp\"
    android:height=\"24dp\"
    android:viewportWidth=\"24\"
    android:viewportHeight=\"24\">
  <path
      android:fillColor=\"@color/colorTextPrimary\"
      android:pathData=\"M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z\"/>
</vector>'''

def write_file(path, content):
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

for name, content in drawables.items():
    write_file(os.path.join(drawable_dir, name), content)

for name in vectors:
    write_file(os.path.join(drawable_dir, name), vector_template)

print("Drawables written.")
