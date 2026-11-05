# Third-party notices

The application depends on [DexKit 2.3.0](https://github.com/LuckyPray/DexKit/tree/2.3.0)
(`org.luckypray:dexkit:2.3.0`) and its transitive dependencies, including Kotlin
standard library and FlatBuffers. Their respective copyright and license terms
continue to apply. DexKit's Java/Kotlin code is distributed under
[Apache-2.0](https://github.com/LuckyPray/DexKit/blob/2.3.0/LICENSE), while the
native `Core/` is distributed under
[LGPL-3.0](https://github.com/LuckyPray/DexKit/blob/2.3.0/Core/LICENSE).

The project source is available at the links above. The APK's packaged native
DexKit library is provided as a separate shared library. Anyone distributing a
binary should review and satisfy the licenses of all bundled components,
including the applicable source, notice and replacement requirements of LGPL.

The [libxposed API](https://github.com/libxposed/api/tree/102.0.0) is used at
compile time under Apache-2.0 and is supplied by the Xposed framework at runtime.
