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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms of Service") },
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
            Text("District One Terms of Service", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Version 1.0 • Last Updated: July 2026", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LegalSection("Acceptable Use", "You agree to use District One for lawful purposes only and in a way that does not infringe the rights of, restrict or inhibit anyone else's use and enjoyment of the service.")
            LegalSection("Community Rules", "Harassment, hate speech, and spam are strictly prohibited. We reserve the right to remove any content that violates these community standards.")
            LegalSection("Content Ownership", "You retain ownership of the content you post on District One. However, by posting, you grant us a non-exclusive license to display and distribute that content within the platform.")
            LegalSection("Reporting Abuse", "Users can report posts, notices, or other members for violations. Reported content will be reviewed by moderators.")
            LegalSection("Account Suspension", "We may suspend or terminate your account if you violate these terms or engage in behavior harmful to the community.")
            LegalSection("Disclaimer", "The service is provided 'as is' without any warranties of any kind. We do not guarantee the accuracy of information posted by users.")
            LegalSection("Limitation of Liability", "District One shall not be liable for any indirect, incidental, special, consequential or punitive damages resulting from your use of the service.")
            LegalSection("Contact Information", "For legal inquiries, please contact us at legal@districtone.example.com")
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
