## Xsocks for Android

A [xsocks](https://github.com/lparam/xsocks) client for Android.

### PREREQUISITES

* JDK 1.8+
* Android SDK r22+
* Android NDK r9+

### BUILD

* Set environment variable `ANDROID_HOME`
* Set environment variable `ANDROID_NDK_HOME`
* Create your key following the instructions at http://developer.android.com/guide/publishing/app-signing.html#cert
* Create your sign.gradle file like this
```bash
    android {
        signingConfigs {
            release {
                storeFile file('/home/user/keystore/android.key')
                    storePassword "password"
                    keyAlias 'Android App Key'
                    keyPassword "password"
            }
        }
    }
```
* Invoke the instructions
```bash
    git submodule update --init
    make
    gradle clean assembleRelease
```

### LICENSE

Copyright (C) 2015 Ken <ken.i18n@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
