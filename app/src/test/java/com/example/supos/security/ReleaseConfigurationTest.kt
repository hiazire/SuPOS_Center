package com.example.supos.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseConfigurationTest {

    private val buildGradleFile = File("build.gradle.kts")
    private val gitignoreFile = File("../.gitignore")
    private val manifestFile = File("src/main/AndroidManifest.xml")
    private val ciWorkflowFile = File("../.github/workflows/android_ci.yml")
    private val releaseChecklistFile = File("../RELEASE_CHECKLIST.md")

    @Test
    fun buildGradle_applicationIdAndFlavorsProperlyConfigured() {
        assertTrue("app/build.gradle.kts 必須存在", buildGradleFile.exists())
        val content = buildGradleFile.readText()

        // 驗證正式 Application ID 不再是 com.example.supos
        assertTrue("defaultConfig 的 applicationId 必須為 com.supos.center",
            content.contains("applicationId = \"com.supos.center\""))
        assertFalse("defaultConfig 絕不可殘留 com.example.supos 作為 applicationId",
            content.contains("applicationId = \"com.example.supos\""))

        // 驗證環境維度與 Flavors
        assertTrue("必須包含 environment flavorDimension", content.contains("flavorDimensions += \"environment\""))
        assertTrue("必須定義 staging flavor", content.contains("create(\"staging\")"))
        assertTrue("staging 必須有 .staging suffix", content.contains("applicationIdSuffix = \".staging\""))
        assertTrue("staging 必須有 -staging versionNameSuffix", content.contains("versionNameSuffix = \"-staging\""))

        assertTrue("必須定義 production flavor", content.contains("create(\"production\")"))
        assertTrue("production 必須定義正式 App 名稱", content.contains("manifestPlaceholders[\"appName\"] = \"SuPOS 中控臺\""))

        // 驗證安全簽署降級
        assertTrue("必須配置 release signingConfigs", content.contains("create(\"release\")"))
        assertTrue("release 簽署必須支援 RELEASE_KEYSTORE_PATH 環境變數", content.contains("RELEASE_KEYSTORE_PATH"))
        assertTrue("缺少金鑰時必須安全降級回退至 debug 簽署", content.contains("getByName(\"debug\")"))

        // 驗證 Lint 嚴格設定與 BuildConfig
        assertTrue("buildConfig 必須開啟", content.contains("buildConfig = true"))
        assertTrue("lint 必須配置 abortOnError = true", content.contains("abortOnError = true"))
    }

    @Test
    fun gitignore_strictlyExcludesKeystoresAndSecrets() {
        assertTrue(".gitignore 必須存在", gitignoreFile.exists())
        val content = gitignoreFile.readText()

        assertTrue("必須排除 *.keystore", content.contains("*.keystore"))
        assertTrue("必須排除 *.jks", content.contains("*.jks"))
        assertTrue("必須排除 *.p12", content.contains("*.p12"))
        assertTrue("必須排除 *.key", content.contains("*.key"))
        assertTrue("必須排除 *.pem", content.contains("*.pem"))
        assertTrue("必須排除 signing.properties", content.contains("signing.properties"))
        assertTrue("必須排除 secrets.properties", content.contains("secrets.properties"))
    }

    @Test
    fun manifest_declaresRequiredPermissionsAndDynamicLabel() {
        assertTrue("AndroidManifest.xml 必須存在", manifestFile.exists())
        val content = manifestFile.readText()

        assertTrue("必須宣告 ACCESS_NETWORK_STATE 權限以供 NetworkMonitor 使用",
            content.contains("android.permission.ACCESS_NETWORK_STATE"))
        assertTrue("必須宣告 INTERNET 權限",
            content.contains("android.permission.INTERNET"))
        assertTrue("應用標籤必須使用 \${appName} 動態佔位符",
            content.contains("android:label=\"\${appName}\""))
    }

    @Test
    fun ciWorkflow_definesBuildLintTestAndAssembleMatrix() {
        assertTrue(".github/workflows/android_ci.yml 必須存在", ciWorkflowFile.exists())
        val content = ciWorkflowFile.readText()

        assertTrue("CI 必須執行 secret scanning", content.contains("Scanning codebase for accidental secrets"))
        assertTrue("CI 必須執行 Lint 檢查", content.contains("lintStagingDebug"))
        assertTrue("CI 必須執行單元測試矩陣", content.contains("testStagingDebugUnitTest"))
        assertTrue("CI 必須執行 assembleStagingRelease", content.contains("assembleStagingRelease"))
        assertTrue("CI 必須執行 assembleProductionRelease", content.contains("assembleProductionRelease"))
    }

    @Test
    fun releaseChecklist_definesSopAndRollbackProcedures() {
        assertTrue("RELEASE_CHECKLIST.md 必須存在", releaseChecklistFile.exists())
        val content = releaseChecklistFile.readText()

        assertTrue("必須包含語意化版本與 Version Code 定義", content.contains("versionCode"))
        assertTrue("必須包含 Staging 驗收清單", content.contains("Staging 驗收檢查清單"))
        assertTrue("必須包含 Production 發布程序", content.contains("Production 發布程序"))
        assertTrue("必須包含緊急回滾標準作業程序", content.contains("緊急回滾標準作業程序"))
        assertTrue("回滾程序必須強調 Room 資料庫向後相容與非破壞性原則", content.contains("Room 資料庫向後相容"))
    }
}
