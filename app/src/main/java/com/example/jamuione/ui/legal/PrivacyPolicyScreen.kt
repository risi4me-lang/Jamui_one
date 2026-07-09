package com.example.jamuione.ui.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("District One Privacy Policy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Version 1.0 • Last Updated: July 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LegalSection("Information We Collect", "We collect information you provide directly to us, such as when you create or modify your account, participate in any interactive features of our services, or communicate with us.")
            LegalSection("User Generated Content", "Any content you post, including posts, notices, and comments, is stored on our servers and visible to other members of your community.")
            LegalSection("Location Information", "We collect your current and native location details to provide hyper-local content and community discovery features.")
            LegalSection("Authentication", "We use Firebase Authentication for secure sign-in via email/password or Google.")
            LegalSection("Analytics", "We use Firebase Analytics and Crashlytics to monitor app performance and improve your experience.")
            LegalSection("Data Retention", "We retain your data as long as your account is active. You can request deletion at any time via the Profile settings.")
            LegalSection("Account Deletion", "When you delete your account, your profile is removed. Your posts and comments are 'soft-deleted' (anonymized) to preserve community discussions.")
            LegalSection("Contact Information", "For privacy-related inquiries, please contact us at privacy@districtone.example.com")
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun LegalSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = content, style = MaterialTheme.typography.bodyMedium)
    }
}
