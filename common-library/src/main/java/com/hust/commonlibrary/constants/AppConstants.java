package com.hust.commonlibrary.constants;

public final class AppConstants {
    private AppConstants() {
        // Prevent instantiation
    }

    public static final String DEFAULT_LANGUAGE = "vi";
    public static final String SYSTEM = "system";

    public static final String BACKEND_PREFIX = "/api/v1";
    public static final String PHONE_NUMBER = "^\\+?[0-9]{10,15}$";

    public static final String FRONTEND_HOST = "http://localhost:3000";
    public static final String BACKEND_HOST = "http://localhost:8088";

    public static final String DEFAULT_PAGE_NUMBER = "1";
    public static final String DEFAULT_PAGE_SIZE = "15";
    public static final String DEFAULT_SORT = "id,desc";

    public static final String DEFAULT_AVATAR = "https://th.bing.com/th/id/OIP.dDKYQqVBsG1tIt2uJzEJHwHaHa?w=182&h=182&c=7&r=0&o=7&pid=1.7&rm=3";
    public static final int DEFAULT_MAX_TOKEN_NUM = 3;

    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    public static final String INTERNAL_SECRET_VALUE = "e-learning-internal-secure-secret-key-123456";

    @SuppressWarnings("java:S2386")
    public static final String[] PUBLIC_API_PATH = {
            // Identity Service
            "/auth/login",
            "/auth/register",
            "/auth/register-instructor",
            "/auth/logout",
            "/auth/refresh-token",
            "/users/authors/select",

            // Course Service
            "/courses/search",
            "/courses/search-elasticsearch",
            "/courses/suggest",
            "/courses/popular",
            "/courses/trending",
            "/courses/recommended",
            "/courses/related/**",
            "/courses/all-active",
            "/courses/count-by-categories",
            "/courses/select",
            "/courses/{id}",
            "/courses/ids",
            "/courses/bulk",
            "/courses/increase-view/{id}",
            "/courses/{id}/view",
            "/categories/search",
            "/categories/select",
            "/categories/{id}",
            "/sections/course/{courseId}",
            "/sections/{id}",
            "/lessons/section/{sectionId}",
            "/lessons/course/{courseId}/all-lessons",
            "/lessons/{id}",

            // Learning Service
            "/gamification/leaderboard",
            "/gamification/users/{userId}",
            "/gamification/users/{userId}/activities",

            // Interaction Service
            "/blogs",
            "/blogs/featured",
            "/blogs/{slug}",
            "/blogs/topics/search",
            "/blogs/topics/select",
            "/blogs/topics/{id}",
            "/blogs/{id}/comments",
            "/reviews/course/{courseId}",
            "/reviews/course/{courseId}/summary",
            "/reviews/{reviewId}/replies",
            "/feedbacks",

            // Order & Payment Service
            "/orders/public/test-saga",
            "/payments/vnpay/**"
    };

    @SuppressWarnings("java:S101")
    public static final class Token_Constants {
        private Token_Constants() {
            // Prevent instantiation
        }
        public static final String ACCESS_TOKEN = "access_token";
        public static final String REFRESH_TOKEN = "refresh_token";
    }

    @SuppressWarnings("java:S101")
    public static final class Redis_Constants {
        private Redis_Constants() {
            // Prevent instantiation
        }
        public static final String APP_PREFIX = "elearning:";
    }

    @SuppressWarnings("java:S101")
    public static final class Role_Constants {
        private Role_Constants() {
            // Prevent instantiation
        }
        public static final String ROLE_STUDENT = "STUDENT";
        public static final String ROLE_INSTRUCTOR = "INSTRUCTOR";
        public static final String ROLE_ADMIN = "ADMIN";
    }

    @SuppressWarnings("java:S101")
    public static final class Notification_Constants {
        private Notification_Constants() {
            // Prevent instantiation
        }
        public static final String ALL_ADMIN_TOPIC = "ALL_ADMIN";
    }

    @SuppressWarnings("java:S101")
    public static final class Field_Constants {
        private Field_Constants() {
            // Prevent instantiation
        }
        public static final String ID = "id";
        public static final String SLUG = "slug";
        public static final String NAME = "name";
        public static final String EMAIL = "email";
        public static final String CODE = "code";
    }

    @SuppressWarnings("java:S101")
    public static final class Resource_Constants {
        private Resource_Constants() {
            // Prevent instantiation
        }
        public static final String COURSE = "Course";
        public static final String USER = "User";
        public static final String CATEGORY = "Category";
        public static final String LESSON = "Lesson";
        public static final String ROLE = "Role";
        public static final String SESSION = "Session";
        public static final String TOPIC = "Topic";
        public static final String BLOG = "Blog";
        public static final String COMMENT = "Comment";
    }

    @SuppressWarnings("java:S101")
    public static final class Upload_Strategies {
        private Upload_Strategies() {
            // Prevent instantiation
        }
        public static final String S3 = "s3";
    }
}
