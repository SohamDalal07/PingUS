package com.example.backtoyou;

public class UserProfile {

    private final String name;
    private final String email;
    private final String role;
    private final String studentId;
    private final String department;
    private final String phoneNumber;
    private final int postsCount;
    private final int alertsCount;

    public UserProfile(String name, String email, String role, String studentId,
                       String department, String phoneNumber, int postsCount, int alertsCount) {
        this.name = name;
        this.email = email;
        this.role = role;
        this.studentId = studentId;
        this.department = department;
        this.phoneNumber = phoneNumber;
        this.postsCount = postsCount;
        this.alertsCount = alertsCount;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStudentId() { return studentId; }
    public String getDepartment() { return department; }
    public String getPhoneNumber() { return phoneNumber; }
    public int getPostsCount() { return postsCount; }
    public int getAlertsCount() { return alertsCount; }

    public String getDepartmentShortCode() {
        if (department == null || department.trim().isEmpty()) {
            return "--";
        }
        String[] parts = department.trim().split("\\s+");
        StringBuilder code = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                code.append(Character.toUpperCase(part.charAt(0)));
            }
            if (code.length() == 2) {
                break;
            }
        }
        return code.length() > 0 ? code.toString() : "--";
    }
}
