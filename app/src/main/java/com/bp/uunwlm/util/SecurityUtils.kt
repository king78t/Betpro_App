package com.bp.uunwlm.util

import org.mindrot.jbcrypt.BCrypt

object SecurityUtils {
    
    /**
     * Hashes a plain text password using BCrypt.
     */
    fun hashPassword(plainText: String): String {
        return BCrypt.hashpw(plainText, BCrypt.gensalt())
    }

    /**
     * Verifies a plain text password against a hashed password.
     */
    fun verifyPassword(plainText: String, hashed: String): Boolean {
        return try {
            BCrypt.checkpw(plainText, hashed)
        } catch (e: Exception) {
            false
        }
    }
}
