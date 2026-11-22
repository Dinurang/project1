package com.suntravels.dto;

import com.suntravels.model.Role;

// Request payload for account registration; role-specific fields are validated by AuthService.
public class SignupRequest {
    private String username;
    private String email;
    private String password;
    private Role role;
    private String fullName;

    // Contract Administrator fields
    private String employeeCode;
    private String contactNo;

    // Call Center Agent fields
    private String agentCode;
    private String deskExtension;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }

    public String getContactNo() { return contactNo; }
    public void setContactNo(String contactNo) { this.contactNo = contactNo; }

    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }

    public String getDeskExtension() { return deskExtension; }
    public void setDeskExtension(String deskExtension) { this.deskExtension = deskExtension; }
}
