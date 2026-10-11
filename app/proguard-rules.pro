# Room locates this generated implementation from the database class name.
# Keep only this reflective boundary; app/UI/library code remains optimizable.
-keep,allowoptimization class com.omni.noteplus.data.AppDatabase { *; }
-keep,allowoptimization class com.omni.noteplus.data.AppDatabase_Impl { *; }

# Retain useful crash positions alongside the per-release R8 mapping artifact.
-keepattributes SourceFile,LineNumberTable
