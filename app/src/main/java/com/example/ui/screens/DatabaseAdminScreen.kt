package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.DbConnectionProfile
import com.example.viewmodel.ApiState
import com.example.viewmodel.ZiskareViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SystemSuccess
import com.example.ui.theme.SystemError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseAdminScreen(
    viewModel: ZiskareViewModel,
    modifier: Modifier = Modifier
) {
    val connectionsState by viewModel.dbConnections.collectAsState()
    val databasesState by viewModel.databases.collectAsState()
    val collectionsState by viewModel.collectionsObj.collectAsState()
    val dataState by viewModel.collectionData.collectAsState()

    var selectedDb by remember { mutableStateOf<String?>(null) }
    var selectedCollection by remember { mutableStateOf<String?>(null) }

    var showAddDbDialog by remember { mutableStateOf(false) }
    var showAddCollDialog by remember { mutableStateOf(false) }
    var showAddDocDialog by remember { mutableStateOf(false) }

    // Init Data load
    LaunchedEffect(Unit) {
        viewModel.fetchDbConnections()
        viewModel.listDatabases()
    }

    Row(modifier = modifier.fillMaxSize()) {
        // Navigation Left Subpane: Database Tree Explorer
        Column(
            modifier = Modifier
                .width(260.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MONGODB NODES",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Row {
                    IconButton(onClick = { viewModel.listDatabases() }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh DB list", tint = NeonCyan, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showAddDbDialog = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Create Database", tint = SystemSuccess, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // active DB profile
            when (val state = connectionsState) {
                is ApiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally))
                is ApiState.Success -> {
                    Text("ACTIVE MONGO ENDPOINT", style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontSize = 9.sp)
                    state.data.forEach { profile ->
                        val isAct = profile.isActive == true
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAct) NeonCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { profile.id?.let { viewModel.activateDbConnection(it) } }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isAct) SystemSuccess else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(profile.name, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                else -> {}
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Text("DATABASES", style = MaterialTheme.typography.labelMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(6.dp))

            // Databases list
            when (val state = databasesState) {
                is ApiState.Loading -> Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                is ApiState.Success -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(state.data) { db ->
                            val isSel = selectedDb == db
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) RoyalPurple.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            selectedDb = db
                                            selectedCollection = null
                                            viewModel.getCollectionsInDatabase(db)
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (isSel) Icons.Default.FolderOpen else Icons.Default.Folder,
                                            contentDescription = "Db Instance",
                                            tint = if (isSel) RoyalPurple else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(db, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                    }
                                    if (isSel) {
                                        IconButton(
                                            onClick = { viewModel.deleteSystemDatabase(db); selectedDb = null },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Drop database", tint = SystemError, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                if (isSel) {
                                    // Collections child rendering
                                    Column(modifier = Modifier.padding(start = 20.dp, top = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("COLLECTIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                            IconButton(onClick = { showAddCollDialog = true }, modifier = Modifier.size(18.dp)) {
                                                Icon(Icons.Default.Add, contentDescription = "Add collection", tint = SystemSuccess, modifier = Modifier.size(12.dp))
                                            }
                                        }

                                        when (val colState = collectionsState) {
                                            is ApiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                                            is ApiState.Success -> {
                                                colState.data.collections?.forEach { col ->
                                                    val isColSel = selectedCollection == col
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isColSel) NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
                                                            .clickable {
                                                                selectedCollection = col
                                                                viewModel.getCollectionData(db, col)
                                                            }
                                                            .padding(6.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.ListAlt, contentDescription = "Col", tint = if (isColSel) NeonCyan else Color.Gray, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(col, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
                                                        }
                                                        if (isColSel) {
                                                            IconButton(
                                                                onClick = { viewModel.deleteSystemCollection(db, col); selectedCollection = null },
                                                                modifier = Modifier.size(16.dp)
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Drop collection", tint = SystemError, modifier = Modifier.size(12.dp))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is ApiState.Error -> Text("Failed to render nodes: ${state.message}", fontSize = 11.sp, color = SystemError)
                else -> {}
            }
        }

        // Right pane: Table/Documents viewer
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedCollection != null) "DATABASE EXPLORER: $selectedCollection" else "DATABASE EXPLORER",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (selectedDb != null) "Active Schema Workspace: $selectedDb" else "Select structural nodes from sidebar tree context.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (selectedDb != null && selectedCollection != null) {
                    Button(
                        onClick = { showAddDocDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "Add Document", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ADD RECORD")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedCollection == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Storage, contentDescription = "Db", modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No active collection selected", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                        Text("Pick a database and click a child collection to audit.", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            } else {
                when (val dataRes = dataState) {
                    is ApiState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NeonCyan) }
                    is ApiState.Success -> {
                        val docs = dataRes.data.data ?: emptyList()
                        if (docs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("This collection is completely empty. Click 'ADD RECORD' above.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(docs) { doc ->
                                    val idVal = doc["_id"]?.toString() ?: doc["id"]?.toString() ?: ""
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "DOCUMENT: $idVal",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = NeonCyan
                                                )
                                                IconButton(
                                                    onClick = {
                                                        if (idVal.isNotEmpty()) {
                                                            viewModel.deleteCollectionDataEntry(selectedDb!!, selectedCollection!!, idVal)
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove document", tint = SystemError, modifier = Modifier.size(16.dp))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Exposing JSON style parameters
                                            doc.entries.filter { it.key != "_id" && it.key != "id" }.forEach { entry ->
                                                Row(
                                                    modifier = Modifier.padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        entry.key,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = RoyalPurple,
                                                        modifier = Modifier.width(100.dp)
                                                    )
                                                    Text(
                                                        entry.value.toString(),
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ApiState.Error -> Text("Failed to parse document arrays: ${dataRes.message}", color = SystemError)
                    else -> {}
                }
            }
        }
    }

    // Modal creation dialogs
    if (showAddDbDialog) {
        var dbNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDbDialog = false },
            title = { Text("PROVISION NEW DATABASE SCHEMA") },
            text = {
                OutlinedTextField(
                    value = dbNameInput,
                    onValueChange = { dbNameInput = it },
                    label = { Text("Database Schema Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dbNameInput.isNotEmpty()) {
                            viewModel.createDatabase(dbNameInput)
                        }
                        showAddDbDialog = false
                    }
                ) { Text("CREATE") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDbDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showAddCollDialog && selectedDb != null) {
        var colInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCollDialog = false },
            title = { Text("CREATE MONGODB COLLECTION") },
            text = {
                OutlinedTextField(
                    value = colInput,
                    onValueChange = { colInput = it },
                    label = { Text("Collection Table Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (colInput.isNotEmpty()) {
                            viewModel.createCollection(selectedDb!!, colInput)
                        }
                        showAddCollDialog = false
                    }
                ) { Text("PROVISION") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCollDialog = false }) { Text("CANCEL") }
            }
        )
    }

    if (showAddDocDialog && selectedDb != null && selectedCollection != null) {
        var customKey by remember { mutableStateOf("") }
        var customVal by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDocDialog = false },
            title = { Text("INSERT COLLECTION DOCUMENT") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Creates a simple administrative test record mapping fields cleanly.", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = customKey,
                        onValueChange = { customKey = it },
                        label = { Text("Attribute Property Key (e.g., name)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customVal,
                        onValueChange = { customVal = it },
                        label = { Text("Attribute String Value (e.g., prod-db-core)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customKey.isNotEmpty()) {
                            val map = mapOf(customKey to customVal, "created_at" to System.currentTimeMillis())
                            viewModel.addDocumentToCollection(selectedDb!!, selectedCollection!!, map)
                        }
                        showAddDocDialog = false
                    }
                ) { Text("INSERT RECORD") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDocDialog = false }) { Text("CANCEL") }
            }
        )
    }
}
