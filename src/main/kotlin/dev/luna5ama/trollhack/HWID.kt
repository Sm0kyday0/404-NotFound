package dev.luna5ama.trollhack

import java.security.MessageDigest

object HWID {
    fun getHWID(): String {
        return try {
            val toEncrypt = System.getenv("COMPUTERNAME") + System.getProperty("user.name") +
                    System.getenv("PROCESSOR_IDENTIFIER") + System.getenv("PROCESSOR_LEVEL")
            val md = MessageDigest.getInstance("MD5")
            md.update(toEncrypt.toByteArray())
            val byteData = md.digest()
            val hexString = StringBuilder()

            for (byte in byteData) {
                val hex = String.format("%02x", byte)
                hexString.append(hex)
            }

            hexString.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error"
        }
    }
}