package com.example.supos.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BackupSecurityPolicyTest {

    private val manifestFile = File("src/main/AndroidManifest.xml")
    private val dataExtractionFile = File("src/main/res/xml/data_extraction_rules.xml")
    private val backupRulesFile = File("src/main/res/xml/backup_rules.xml")

    @Test
    fun manifest_allowBackupIsExplicitlyFalse() {
        assertTrue("AndroidManifest.xml 必須存在", manifestFile.exists())
        val manifestContent = manifestFile.readText()

        assertTrue(
            "AndroidManifest.xml 必須顯式配置 android:allowBackup=\"false\" 防止商用 POS 資料外洩",
            manifestContent.contains("android:allowBackup=\"false\"")
        )
        assertFalse(
            "AndroidManifest.xml 絕不可設置 android:allowBackup=\"true\"",
            manifestContent.contains("android:allowBackup=\"true\"")
        )
    }

    @Test
    fun manifest_backupRulesAreExplicitlyMounted() {
        val manifestContent = manifestFile.readText()

        assertTrue(
            "AndroidManifest.xml 必須掛載 android:dataExtractionRules=\"@xml/data_extraction_rules\"",
            manifestContent.contains("android:dataExtractionRules=\"@xml/data_extraction_rules\"")
        )
        assertTrue(
            "AndroidManifest.xml 必須掛載 android:fullBackupContent=\"@xml/backup_rules\"",
            manifestContent.contains("android:fullBackupContent=\"@xml/backup_rules\"")
        )
    }

    @Test
    fun dataExtractionRules_excludesSensitiveDomainsAndDatabases() {
        assertTrue("data_extraction_rules.xml 必須存在", dataExtractionFile.exists())
        val content = dataExtractionFile.readText()

        // 必須包含 cloud-backup 與 device-transfer 節點
        assertTrue(content.contains("<cloud-backup>"))
        assertTrue(content.contains("<device-transfer>"))

        // 必須排除資料庫
        assertTrue(
            "必須排除 Room 資料庫 supos_database",
            content.contains("<exclude domain=\"database\" path=\"supos_database\"")
        )
        // 必須排除 sharedpref 與根目錄
        assertTrue(
            "必須排除偏好設定 sharedpref",
            content.contains("<exclude domain=\"sharedpref\"")
        )
        assertTrue(
            "必須排除本地檔案與根目錄",
            content.contains("<exclude domain=\"root\"") && content.contains("<exclude domain=\"file\"")
        )

        // 絕不允許無限制 include
        assertFalse(
            "不可無條件 include 敏感儲存域",
            content.contains("<include domain=\"database\"") || content.contains("<include domain=\"root\"")
        )
    }

    @Test
    fun backupRules_excludesSensitiveDomainsAndDatabases() {
        assertTrue("backup_rules.xml 必須存在", backupRulesFile.exists())
        val content = backupRulesFile.readText()

        assertTrue(content.contains("<full-backup-content>"))

        // 必須排除資料庫
        assertTrue(
            "舊版備份規則必須排除 Room 資料庫 supos_database",
            content.contains("<exclude domain=\"database\" path=\"supos_database\"")
        )
        // 必須排除 sharedpref 與根目錄
        assertTrue(
            "舊版備份規則必須排除偏好設定 sharedpref",
            content.contains("<exclude domain=\"sharedpref\"")
        )
        assertTrue(
            "舊版備份規則必須排除根目錄與檔案",
            content.contains("<exclude domain=\"root\"") && content.contains("<exclude domain=\"file\"")
        )

        // 絕不允許無限制 include
        assertFalse(
            "不可無條件 include 敏感儲存域",
            content.contains("<include domain=\"database\"") || content.contains("<include domain=\"root\"")
        )
    }
}
