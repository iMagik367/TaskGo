# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preservar line numbers e atributos importantes para stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature
-keepattributes Exceptions

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Firebase App Check
-keep class com.google.firebase.appcheck.** { *; }
-keep class com.google.firebase.appcheck.debug.** { *; }
-keep class com.google.firebase.appcheck.playintegrity.** { *; }

# Play Integrity (necessário para App Check em release)
-keep class com.google.android.play.core.integrity.** { *; }
-keep class com.google.android.gms.playintegrity.** { *; }
-dontwarn com.google.android.play.core.integrity.**
-dontwarn com.google.android.gms.playintegrity.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper <methods>;
}

# Hilt Modules - manter todos os módulos e providers
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @javax.inject.Singleton class * { *; }
-keepclassmembers class * {
    @dagger.Provides <methods>;
    @dagger.Binds <methods>;
}

# Hilt Generated Classes - manter classes geradas pelo Hilt
-keep class com.taskgoapp.taskgo.Hilt_* { *; }

# Manter Application e Activities com Hilt
-keep @dagger.hilt.android.HiltAndroidApp class * extends android.app.Application { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.app.Activity { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends androidx.fragment.app.Fragment { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends androidx.fragment.app.DialogFragment { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.view.View { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.view.ViewGroup { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.widget.ViewAnimator { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class * extends android.widget.ViewSwitcher { *; }

# Manter classes principais do app - CRÍTICO: Preservar completamente TaskGoApp e sua classe gerada pelo Hilt
-keep class com.taskgoapp.taskgo.TaskGoApp { *; }
-keep class com.taskgoapp.taskgo.Hilt_TaskGoApp { *; }
-keepclassmembers class com.taskgoapp.taskgo.TaskGoApp {
    <init>(...);
    <methods>;
    <fields>;
}
-keepclassmembers class com.taskgoapp.taskgo.Hilt_TaskGoApp {
    <init>(...);
    <methods>;
    <fields>;
}
-keep class com.taskgoapp.taskgo.MainActivity { *; }

# Hilt WorkManager e Workers - CRÍTICO: Preservar workers e seus construtores AssistedInject
-keep class androidx.hilt.work.** { *; }
-keep class com.taskgoapp.taskgo.core.sync.SyncWorker { *; }
-keep class com.taskgoapp.taskgo.core.work.AccountChangeProcessorWorker { *; }
-keep class com.taskgoapp.taskgo.core.sync.SyncWorker$* { *; }
-keep class com.taskgoapp.taskgo.core.work.AccountChangeProcessorWorker$* { *; }
-keepclassmembers class com.taskgoapp.taskgo.core.sync.SyncWorker {
    <init>(...);
    <methods>;
    <fields>;
}
-keepclassmembers class com.taskgoapp.taskgo.core.work.AccountChangeProcessorWorker {
    <init>(...);
    <methods>;
    <fields>;
}
-keep class dagger.assisted.** { *; }
-keep @dagger.assisted.AssistedInject class * { *; }
-keepclassmembers class * {
    @dagger.assisted.AssistedInject <init>(...);
}
# Manter todas as classes HiltWorker para evitar problemas de instanciação
-keep @androidx.hilt.work.HiltWorker class * extends androidx.work.CoroutineWorker { *; }
-keep @androidx.hilt.work.HiltWorker class * extends androidx.work.Worker { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keep class okhttp3.internal.platform.ConscryptPlatform

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# Navigation Compose com Hilt
-keep class androidx.navigation.** { *; }
-keep class androidx.hilt.navigation.compose.** { *; }

# Data classes do projeto - manter para serialização
-keep class com.taskgoapp.taskgo.data.firestore.models.** { *; }
-keep class com.taskgoapp.taskgo.core.model.** { *; }
-keep class com.taskgoapp.taskgo.data.local.** { *; }

# ViewModels e UiState - manter ViewModels com Hilt (regra específica primeiro)
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.ViewModel$* { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil
-dontwarn coil.**
-keep class coil.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# SLF4J
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Google Tink (necessário para Nimbus JOSE)
# Manter todas as classes do Tink sem ofuscar devido a bug no R8
-keep class com.google.crypto.tink.** { *; }
-keep class com.google.crypto.tink.subtle.** { *; }
-keep class com.google.crypto.tink.shaded.** { *; }
-keepclassmembers class com.google.crypto.tink.** { *; }
-keepclassmembers class com.google.crypto.tink.shaded.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.crypto.tink.subtle.XChaCha20Poly1305

# Nimbus JOSE (JWT)
-dontwarn com.nimbusds.jose.**
-keep class com.nimbusds.jose.** { *; }
-keep class com.nimbusds.jwt.** { *; }

# R8 Fix - Evitar ClassCastException LinkedList to z2
# Solução conservadora: Manter apenas estruturas essenciais sem desabilitar otimizações
# Isso evita crash do JVM durante a compilação R8 mantendo funcionalidade
-keep class java.util.LinkedList { *; }
-keep class java.util.ArrayList { *; }
-keep class java.util.List { *; }
-keep class java.util.Collection { *; }
-keep class java.util.AbstractList { *; }
-keep class java.util.AbstractCollection { *; }
# Preservar atributos essenciais para stack traces e reflexão
-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod,SourceFile,LineNumberTable

# Proteger classes de validação para evitar problemas durante análise estática do R8
-keep class com.taskgoapp.taskgo.core.validation.DocumentValidator { *; }
-keep class com.taskgoapp.taskgo.core.validation.PasswordValidator { *; }
-keep class com.taskgoapp.taskgo.core.validation.CepService { *; }
-keep class com.taskgoapp.taskgo.core.utils.TextFormatters { *; }
# Proteger Regex para evitar problemas durante análise estática
-keep class kotlin.text.Regex { *; }
-keepclassmembers class kotlin.text.Regex { *; }

# Proteger classes de repositório de mídia para evitar problemas durante análise estática
-keep class com.taskgoapp.taskgo.data.repository.FeedMediaRepository { *; }
-keepclassmembers class com.taskgoapp.taskgo.data.repository.FeedMediaRepository { *; }
# Proteger métodos de upload que podem causar problemas durante análise estática
-keepclassmembers class com.taskgoapp.taskgo.data.repository.FeedMediaRepository {
    <methods>;
}
# Proteger Uri e ContentResolver para evitar problemas durante análise estática
-keep class android.net.Uri { *; }
-keep class android.content.ContentResolver { *; }
-keep class android.os.ParcelFileDescriptor { *; }

# R8 Fix - Corrigir erro de referência java.lang.Obzect (typo detectado pelo R8)
# Adicionado conforme sugerido pelo R8 em missing_rules.txt
-dontwarn java.lang.Obzect

# Se você usa WebView com JS, descomente:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
