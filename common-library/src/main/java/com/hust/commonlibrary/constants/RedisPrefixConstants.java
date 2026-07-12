package com.hust.commonlibrary.constants;

/**
 * Centralized definitions for all Redis Key Prefixes and Namespace Schemas across the platform.
 */
public final class RedisPrefixConstants {

    // Prevents instantiation
    private RedisPrefixConstants() {}

    // ==================================================================================
    // GLOBAL SHARED NAMESPACES
    // Must explicitly start with AppConstants.Redis_Constants.APP_PREFIX ("elearning:")
    // to bypass automatic Application Name isolation in RedisServiceImpl!
    // ==================================================================================

    /**
     * Namespace base for global shared course-ownership authorization.
     */
    public static final String SHARED_AUTH_COURSE_BASE = AppConstants.Redis_Constants.APP_PREFIX + "shared:auth:course:";

    /**
     * Namespace base for global shared user profile cache.
     */
    public static final String SHARED_USER_PROFILE_BASE = AppConstants.Redis_Constants.APP_PREFIX + "shared:user:profile:";

    // ==================================================================================
    // LOCAL SERVICE NAMESPACES
    // Prepend local operations. Automatically scoped to applicationName by RedisServiceImpl.
    // ==================================================================================

    /**
     * Identity Service: Blacklisted token keyspace.
     */
    public static final String IDENTITY_BLACKLIST = "blacklist:";

    /**
     * Namespace base for global shared token blacklist.
     */
    public static final String SHARED_BLACKLIST_TOKEN_BASE = AppConstants.Redis_Constants.APP_PREFIX + "shared:blacklist:token:";

    /**
     * Namespace base for global shared user status blacklist.
     */
    public static final String SHARED_BLACKLIST_USER_BASE = AppConstants.Redis_Constants.APP_PREFIX + "shared:blacklist:user:";

    /**
     * Identity Service: Spring Cache profile keyspace.
     */
    public static final String CACHE_USER_PROFILE = "profile::";

    /**
     * Learning Service: Write-behind cache for video watch progress.
     */
    public static final String LOCAL_LESSON_PROGRESS_SYNC = "progress:sync:";

    // ==================================================================================
    // UTILITY KEY BUILDERS
    // Safe helper methods to prevent manual string-concatenation errors.
    // ==================================================================================

    /**
     * Safely constructs the global permanent course ownership key:
     * Format: elearning:shared:auth:course:{courseId}:owner
     */
    public static String getSharedCourseOwnerKey(String courseId) {
        return SHARED_AUTH_COURSE_BASE + courseId + ":owner";
    }

    /**
     * Safely constructs the global shared user profile key:
     * Format: elearning:shared:user:profile:{userId}
     */
    public static String getSharedUserProfileKey(String userId) {
        return SHARED_USER_PROFILE_BASE + userId;
    }

    /**
     * Safely constructs blacklisted token key:
     * Format: blacklist:{token}
     */
    public static String getTokenBlacklistKey(String token) {
        return IDENTITY_BLACKLIST + token;
    }

    /**
     * Safely constructs global shared blacklisted token key:
     * Format: elearning:shared:blacklist:token:{token}
     */
    public static String getSharedTokenBlacklistKey(String token) {
        return SHARED_BLACKLIST_TOKEN_BASE + token;
    }

    /**
     * Safely constructs global shared blacklisted user key:
     * Format: elearning:shared:blacklist:user:{userId}
     */
    public static String getSharedUserBlacklistKey(String userId) {
        return SHARED_BLACKLIST_USER_BASE + userId;
    }

    /**
     * Safely constructs the local lesson progress sync key:
     * Format: progress:sync:{userId}:{courseId}:{lessonId}
     */
    public static String getLessonProgressSyncKey(String userId, String courseId, String lessonId) {
        return LOCAL_LESSON_PROGRESS_SYNC + userId + ":" + courseId + ":" + lessonId;
    }
}
