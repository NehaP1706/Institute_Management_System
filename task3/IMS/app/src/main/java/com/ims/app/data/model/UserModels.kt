package com.ims.app.data.model

import java.util.Date

/** UML: Person */
data class Person(
    val personId: String,
    val name: String,
    val email: String,
    val phone: String,
    val photoUrl: String,
    val dateOfBirth: Date
) {
    fun getFullName(): String = name
    fun getContactInfo(): List<String> = listOf(email, phone)
    fun updateProfile(details: Map<String, Any>) { /* stub */ }
}

/** UML: Guardian extends Person */
data class Guardian(
    val guardianId: String,
    val person: Person,
    val relationship: String,
    val isPrimary: Boolean,
    val address: String
) {
    fun getWards(): List<Student> = emptyList()          // stub
    fun receiveNotification(alert: Any) { /* stub */ }
}

/** UML: Permission */
data class Permission(
    val permissionId: String,
    val module: Module,
    val action: AccessLevel,
    val accessLevels: List<AccessLevel>
) {
    fun permissionIncludedIn(): List<Role> = emptyList() // stub
}

/** UML: Role */
data class Role(
    val roleId: String,
    val roleName: String,
    val permissions: List<Permission>,
    val moduleAccess: Map<Module, AccessLevel>
) {
    fun grantPermission(module: Module, action: AccessLevel) { /* stub */ }
    fun revokePermission(module: Module, action: AccessLevel) { /* stub */ }
    fun canAccess(module: Module): Boolean =
        moduleAccess.containsKey(module)
    fun listPermissions(): List<Permission> = permissions
}

/** UML: User */
data class User(
    val userId: String,
    val passwordHash: String,
    val role: List<Role>,
    val isActive: Boolean,
    val lastLogin: Date,
    val languagePref: String,
    val timezonePref: String
) {
    fun login(credentials: String): User? = null   // stub – real auth handled by StubRepository
    fun logout(sessionToken: String) { /* stub */ }
    fun changePassword(old: String, new: String): Boolean = true
    fun hasPermission(module: Module, action: AccessLevel): Boolean =
        role.any { it.canAccess(module) }
    fun getNotifications(): List<Any> = emptyList()
    fun markAlertRead() { /* stub */ }
    fun applyForLeave(application: LeaveApplication): LeaveApplication = application
}