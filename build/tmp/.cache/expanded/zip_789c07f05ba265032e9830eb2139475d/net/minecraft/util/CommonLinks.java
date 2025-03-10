package net.minecraft.util;

import com.mojang.util.UndashedUuid;
import java.util.UUID;

public class CommonLinks {
    public static final String GDPR = "https://aka.ms/MinecraftGDPR";
    public static final String EULA = "https://aka.ms/MinecraftEULA";
    public static final String PRIVACY_STATEMENT = "http://go.microsoft.com/fwlink/?LinkId=521839";
    public static final String ATTRIBUTION = "https://aka.ms/MinecraftJavaAttribution";
    public static final String LICENSES = "https://aka.ms/MinecraftJavaLicenses";
    public static final String BUY_MINECRAFT_JAVA = "https://aka.ms/BuyMinecraftJava";
    public static final String ACCOUNT_SETTINGS = "https://aka.ms/JavaAccountSettings";
    public static final String SNAPSHOT_FEEDBACK = "https://aka.ms/snapshotfeedback?ref=game";
    public static final String RELEASE_FEEDBACK = "https://aka.ms/javafeedback?ref=game";
    public static final String SNAPSHOT_BUGS_FEEDBACK = "https://aka.ms/snapshotbugs?ref=game";
    public static final String GENERAL_HELP = "https://aka.ms/Minecraft-Support";
    public static final String ACCESSIBILITY_HELP = "https://aka.ms/MinecraftJavaAccessibility";
    public static final String REPORTING_HELP = "https://aka.ms/aboutjavareporting";
    public static final String SUSPENSION_HELP = "https://aka.ms/mcjavamoderation";
    public static final String BLOCKING_HELP = "https://aka.ms/javablocking";
    public static final String SYMLINK_HELP = "https://aka.ms/MinecraftSymLinks";
    public static final String START_REALMS_TRIAL = "https://aka.ms/startjavarealmstrial";
    public static final String BUY_REALMS = "https://aka.ms/BuyJavaRealms";
    public static final String REALMS_TERMS = "https://aka.ms/MinecraftRealmsTerms";
    public static final String REALMS_CONTENT_CREATION = "https://aka.ms/MinecraftRealmsContentCreator";

    public static String extendRealms(String p_276321_, UUID p_301276_, boolean p_276266_) {
        return extendRealms(p_276321_, p_301276_) + "&ref=" + (p_276266_ ? "expiredTrial" : "expiredRealm");
    }

    public static String extendRealms(String p_276318_, UUID p_301122_) {
        return "https://aka.ms/ExtendJavaRealms?subscriptionId=" + p_276318_ + "&profileId=" + UndashedUuid.toString(p_301122_);
    }
}