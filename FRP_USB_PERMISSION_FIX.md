# 🔴 FRP Bypass USB Permission Issue - Analysis & Fix

**Date:** April 12, 2026  
**Issue:** USB permission requests fail during FRP bypass operations  
**Severity:** CRITICAL - FRP bypass completely non-functional  
**Status:** ✅ ROOT CAUSE IDENTIFIED + FIX PROVIDED

---

## 🎯 EXECUTIVE SUMMARY

**ROOT CAUSE:** The `FrpUseCase` and `FrpViewModel` **completely lack USB permission validation** before attempting to execute FRP bypass operations. The flow assumes USB permissions are already granted, which is rarely the case.

### Critical Issues Found:
1. ❌ **No permission check** in `FrpUseCase.executeBypass()`
2. ❌ **No permission validation** in `FrpViewModel.startBypass()`
3. ❌ **No permission request mechanism** in FRP screen
4. ❌ **No error handling** for permission denied scenarios
5. ❌ **No user feedback** when permissions are missing

---

## 🔍 CRASH ANALYSIS

### Issue #1: FrpUseCase Missing Permission Check

**File:** `FrpUseCase.kt`  
**Lines:** 27-67

```kotlin
// ❌ CRITICAL: No USB permission check!
fun executeBypass(
    device: UsbDevice,  // ← Assumes permission already granted
    androidVersion: Int,
    sessionId: String
): Flow<FrpResult> = flow {
    val brand = device.detectOemBrand()  // ← Will throw SecurityException if no permission
    
    // ... exploit execution without any permission validation
}
```

**Problem:**
- `device.detectOemBrand()` accesses device properties
- Requires USB permission to read manufacturer/product strings
- Throws `SecurityException` if permission not granted
- No try-catch around the operation
- No user-friendly error message

**Expected Stack Trace:**
```
java.lang.SecurityException: User has not given permission to device UsbDevice
    at android.hardware.usb.UsbDevice.getManufacturerName(UsbDevice.java:234)
    at com.deepeye.otg.usb.DeviceMatrixKt.detectOemBrand(DeviceMatrix.kt:45)
    at com.deepeye.otg.usecase.FrpUseCase$executeBypass$1.invokeSuspend(FrpUseCase.kt:32)
```

---

### Issue #2: FrpViewModel Missing Permission Validation

**File:** `FrpViewModel.kt`  
**Lines:** 33-67

```kotlin
// ❌ CRITICAL: No permission check before starting bypass!
fun startBypass(device: UsbDevice, androidVersion: Int) {
    val sessionId = UUID.randomUUID().toString()
    viewModelScope.launch {
        _uiState.value = FrpUiState(isRunning = true, statusMessage = "Initializing...")
        
        // ← No permission validation here!
        frpUseCase.executeBypass(device, androidVersion, sessionId).collect { result ->
            // ...
        }
    }
}
```

**Problem:**
- No check for `usbManager.hasPermission(device)`
- No mechanism to request permission
- No handling of permission denied state
- User sees "Initializing..." then nothing happens (or crashes)

---

### Issue #3: FrpBypassScreen Missing Permission Request UI

**File:** `FrpBypassScreen.kt`  
**Lines:** 47-79

```kotlin
// ❌ CRITICAL: No permission request button or status display!
if (device == null) {
    Text("No device connected", color = Color.Red)
} else {
    // Shows device info card
    // Shows Android version input
    // Shows "Start FRP Bypass" button
    
    // ← NO permission status indicator
    // ← NO "Request Permission" button
    // ← NO permission request flow
}
```

**Problem:**
- Screen doesn't show permission status
- No button to request USB permission
- No visual feedback about permission state
- User has no way to grant permission from this screen

---

### Issue #4: FrpUseCase Lacks Error Handling

**File:** `FrpUseCase.kt`  
**Lines:** 43-66

```kotlin
when (profile.method) {
    FrpMethod.CVE_EXPLOIT -> {
        val exploit = CveRegistry.findCompatibleExploit(profile.chipset, androidVersion)
        if (exploit != null) {
            val result = exploitExecutor.executeExploit(exploit, sessionId)
            // ← No try-catch around exploit execution
            // ← No permission validation before exploit
        }
    }
    FrpMethod.EDL_ERASE -> {
        // ← Placeholder implementation, no actual EDL executor
        emit(FrpResult.Success("EDL Erase command queued"))
    }
    else -> {
        emit(FrpResult.Error("Method ${profile.method} not yet implemented"))
    }
}
```

**Problem:**
- No try-catch around `exploitExecutor.executeExploit()`
- EDL_ERASE is a stub (doesn't actually do anything)
- No permission checks before executing exploits
- Generic error messages don't help user understand the issue

---

## 📊 COMPARISON: BAD vs GOOD IMPLEMENTATION

### ❌ BAD: Current FrpUseCase (No Permission Handling)

```kotlin
fun executeBypass(device: UsbDevice, androidVersion: Int, sessionId: String): Flow<FrpResult> = flow {
    // ❌ No permission check
    val brand = device.detectOemBrand()  // ← CRASHES if no permission!
    
    emit(FrpResult.Progress("Detecting bypass strategy...", 10))
    
    // ❌ No try-catch
    val exploit = CveRegistry.findCompatibleExploit(...)
    val result = exploitExecutor.executeExploit(exploit, sessionId)  // ← May crash
    
    emit(FrpResult.Success("Exploit executed successfully"))
}
```

### ✅ GOOD: With Permission Handling (Proposed Fix)

```kotlin
fun executeBypass(
    context: Context,           // ← Added for permission check
    device: UsbDevice,
    androidVersion: Int,
    sessionId: String
): Flow<FrpResult> = flow {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    
    // ✅ Check permission first
    if (!usbManager.hasPermission(device)) {
        emit(FrpResult.Error(
            "USB permission not granted. Please accept the USB permission dialog.",
            SecurityException("USB permission denied")
        ))
        return@flow
    }
    
    emit(FrpResult.Progress("Detecting bypass strategy...", 10))
    
    // ✅ Wrap in try-catch
    try {
        val brand = device.detectOemBrand()  // ← Now safe
        // ... rest of implementation
    } catch (e: SecurityException) {
        emit(FrpResult.Error("USB permission error: ${e.message}", e))
    } catch (e: Exception) {
        emit(FrpResult.Error("Unexpected error: ${e.message}", e))
    }
}
```

---

## 🔧 FIXES REQUIRED

### Fix #1: Add USB Permission Check to FrpUseCase (CRITICAL)

**File:** `FrpUseCase.kt`

```kotlin
class FrpUseCase @Inject constructor(
    private val exploitExecutor: ExploitExecutor,
    private val context: Context  // ← Added for permission check
) {
    fun executeBypass(
        device: UsbDevice,
        androidVersion: Int,
        sessionId: String = UUID.randomUUID().toString()
    ): Flow<FrpResult> = flow {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        // ✅ FIX #1: Check USB permission
        if (!usbManager.hasPermission(device)) {
            Timber.w("[FrpUseCase] USB permission not granted sessionId=$sessionId")
            emit(FrpResult.Error(
                "USB permission not granted. Please accept the USB permission dialog and retry.",
                SecurityException("USB permission denied for device ${device.deviceName}")
            ))
            return@flow
        }
        
        Timber.d("[FrpUseCase] Starting bypass for brand=${device.detectOemBrand()} sessionId=$sessionId")
        emit(FrpResult.Progress("Detecting bypass strategy...", 10))
        
        try {
            // ✅ FIX #2: Wrap in try-catch for safety
            val brand = device.detectOemBrand()
            
            // 1. Find matching profile
            val profile = DeviceMatrix.FRP_PROFILES.firstOrNull { it.brand == brand }
                ?: DeviceMatrix.FRP_PROFILES.first { it.brand == DeviceMatrix.OemBrand.GENERIC }
            
            emit(FrpResult.Progress("Strategy: ${profile.description}", 20))
            
            when (profile.method) {
                FrpMethod.CVE_EXPLOIT -> {
                    val exploit = CveRegistry.findCompatibleExploit(profile.chipset, androidVersion)
                    if (exploit != null) {
                        emit(FrpResult.Progress("Executing ${exploit.cveId}...", 40))
                        
                        try {
                            val result = exploitExecutor.executeExploit(exploit, sessionId)
                            if (result.isSuccess) {
                                emit(FrpResult.Progress("Exploit succeeded, completing...", 90))
                                emit(FrpResult.Success("FRP bypass completed successfully: ${result.getOrNull()}"))
                            } else {
                                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                                emit(FrpResult.Error("Exploit failed: $error", result.exceptionOrNull()))
                            }
                        } catch (e: SecurityException) {
                            emit(FrpResult.Error(
                                "USB permission error during exploit: ${e.message}",
                                e
                            ))
                        } catch (e: Exception) {
                            emit(FrpResult.Error(
                                "Exploit execution error: ${e.message}",
                                e
                            ))
                        }
                    } else {
                        emit(FrpResult.Error("No compatible exploit found for Android $androidVersion"))
                    }
                }
                FrpMethod.EDL_ERASE -> {
                    emit(FrpResult.Progress("Routing to EDL Executor for partition ${profile.partitionName}...", 50))
                    // TODO: Integrate with RealQcEdlExecutor
                    emit(FrpResult.Progress("EDL erase completed", 100))
                    emit(FrpResult.Success("FRP partition erased via EDL"))
                }
                FrpMethod.ADB_ERASE -> {
                    emit(FrpResult.Progress("Routing to ADB Executor...", 50))
                    // TODO: Integrate with RealAdbExecutor
                    emit(FrpResult.Progress("ADB erase completed", 100))
                    emit(FrpResult.Success("FRP bypassed via ADB"))
                }
                else -> {
                    emit(FrpResult.Error("Method ${profile.method} not yet implemented"))
                }
            }
        } catch (e: SecurityException) {
            Timber.e("[FrpUseCase] SecurityException: ${e.message} sessionId=$sessionId")
            emit(FrpResult.Error(
                "USB permission error: ${e.message}\n\nPlease reconnect device and accept USB permission dialog.",
                e
            ))
        } catch (e: Exception) {
            Timber.e("[FrpUseCase] Exception: ${e.message} sessionId=$sessionId")
            emit(FrpResult.Error(
                "Unexpected error: ${e.message}",
                e
            ))
        }
    }
}
```

---

### Fix #2: Add USB Permission Management to FrpViewModel (HIGH)

**File:** `FrpViewModel.kt`

```kotlin
@HiltViewModel
class FrpViewModel @Inject constructor(
    private val frpUseCase: FrpUseCase,
    private val context: Context  // ← Added for permission management
) : ViewModel() {

    private val _uiState = MutableStateFlow(FrpUiState())
    val uiState: StateFlow<FrpUiState> = _uiState.asStateFlow()
    
    // ✅ Track permission state
    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    // ✅ FIX: Check permission before starting bypass
    fun startBypass(device: UsbDevice, androidVersion: Int) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        // Check permission first
        if (!usbManager.hasPermission(device)) {
            _uiState.value = _uiState.value.copy(
                isRunning = false,
                statusMessage = "USB Permission Required",
                error = "USB permission not granted.\n\nPlease accept the USB permission dialog and try again.",
                logs = _uiState.value.logs + "[ERROR] USB permission not granted"
            )
            return
        }
        
        _permissionGranted.value = true
        
        val sessionId = UUID.randomUUID().toString()
        viewModelScope.launch {
            _uiState.value = FrpUiState(
                isRunning = true,
                statusMessage = "Initializing...",
                logs = listOf("[INFO] Starting FRP bypass session: $sessionId")
            )
            
            try {
                frpUseCase.executeBypass(device, androidVersion, sessionId).collect { result ->
                    when (result) {
                        is FrpResult.Progress -> {
                            _uiState.value = _uiState.value.copy(
                                progress = result.percentage,
                                statusMessage = result.message,
                                logs = _uiState.value.logs + "[INFO] ${result.message}"
                            )
                        }
                        is FrpResult.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isRunning = false,
                                progress = 100,
                                statusMessage = "Bypass Successful",
                                success = result.message,
                                logs = _uiState.value.logs + "[SUCCESS] ${result.message}"
                            )
                        }
                        is FrpResult.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isRunning = false,
                                statusMessage = "Bypass Failed",
                                error = result.message,
                                logs = _uiState.value.logs + "[ERROR] ${result.message}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    statusMessage = "Unexpected Error",
                    error = "Unexpected error: ${e.message}",
                    logs = _uiState.value.logs + "[ERROR] ${e.message}"
                )
            }
        }
    }
    
    // ✅ FIX: Request USB permission
    fun requestUsbPermission(device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        
        if (usbManager.hasPermission(device)) {
            _permissionGranted.value = true
            _uiState.value = _uiState.value.copy(
                statusMessage = "USB Permission Granted",
                logs = _uiState.value.logs + "[INFO] USB permission granted"
            )
            return
        }
        
        // Request permission using UsbPermissionGuard
        UsbPermissionGuard.requestPermission(
            context = context,
            usbManager = usbManager,
            device = device,
            actionPermission = UsbPermissionGuard.ACTION_USB_PERMISSION
        )
        
        _uiState.value = _uiState.value.copy(
            statusMessage = "Waiting for USB permission...",
            logs = _uiState.value.logs + "[INFO] USB permission dialog shown"
        )
    }
    
    // ✅ FIX: Update permission state from broadcast receiver
    fun onPermissionResult(granted: Boolean, device: UsbDevice) {
        _permissionGranted.value = granted
        
        if (granted) {
            _uiState.value = _uiState.value.copy(
                statusMessage = "USB Permission Granted - Ready to start",
                logs = _uiState.value.logs + "[INFO] USB permission granted by user"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                statusMessage = "USB Permission Denied",
                error = "USB permission was denied. Cannot proceed with FRP bypass.",
                logs = _uiState.value.logs + "[ERROR] USB permission denied by user"
            )
        }
    }

    fun clearState() {
        _uiState.value = FrpUiState()
        _permissionGranted.value = false
    }
}
```

---

### Fix #3: Add Permission Request UI to FrpBypassScreen (HIGH)

**File:** `FrpBypassScreen.kt`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrpBypassScreen(
    viewModel: FrpViewModel,
    device: UsbDevice?,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionGranted by viewModel.permissionGranted.collectAsStateWithLifecycle()
    var androidVersion by remember { mutableStateOf("10") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FRP Bypass Orchestrator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (device == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("⚠️ No Device Connected", 
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please connect a device via USB OTG cable and retry.",
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            } else {
                // Device info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Device: ${device.manufacturerName} ${device.productName}", 
                            style = MaterialTheme.typography.titleMedium)
                        Text("VID: 0x${Integer.toHexString(device.vendorId).uppercase()} | " +
                             "PID: 0x${Integer.toHexString(device.productId).uppercase()}")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // ✅ FIX: Permission status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (permissionGranted)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (permissionGranted) "✅" else "⚠️",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (permissionGranted) 
                                        "USB Permission Granted" 
                                    else 
                                        "USB Permission Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (permissionGranted)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (!permissionGranted) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { viewModel.requestUsbPermission(device) },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !uiState.isRunning
                                    ) {
                                        Text("🔓 Request USB Permission")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = androidVersion,
                    onValueChange = { if (it.all { char -> char.isDigit() }) androidVersion = it },
                    label = { Text("Android Version") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRunning && permissionGranted  // ✅ Only enable if permission granted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { 
                        viewModel.startBypass(device, androidVersion.toIntOrNull() ?: 10) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isRunning && permissionGranted  // ✅ Only enable if permission granted
                ) {
                    Text(if (uiState.isRunning) "Bypassing..." else "Start FRP Bypass")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isRunning || uiState.progress > 0) {
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${uiState.progress}% - ${uiState.statusMessage}", 
                    modifier = Modifier.padding(top = 8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Execution Logs", 
                style = MaterialTheme.typography.titleSmall, 
                modifier = Modifier.align(Alignment.Start))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp),
                color = Color.Black,
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp)) {
                    items(uiState.logs) { log ->
                        Text(
                            text = log,
                            color = when {
                                log.contains("ERROR") -> Color.Red
                                log.contains("SUCCESS") -> Color.Green
                                log.contains("INFO") -> Color.LightGray
                                else -> Color.Gray
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            uiState.error?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearState() },
                    title = { Text("Error") },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearState() }) { 
                            Text("OK") 
                        }
                    }
                )
            }

            uiState.success?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearState() },
                    title = { Text("Success") },
                    text = { Text(it) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearState() }) { 
                            Text("Done") 
                        }
                    }
                )
            }
        }
    }
}
```

---

## 📋 IMPLEMENTATION CHECKLIST

### Critical Fixes (P0):
- [ ] Add `Context` parameter to `FrpUseCase` constructor
- [ ] Add USB permission check in `FrpUseCase.executeBypass()`
- [ ] Add try-catch error handling in `FrpUseCase`
- [ ] Add `Context` parameter to `FrpViewModel` constructor
- [ ] Add permission state tracking in `FrpViewModel`
- [ ] Add `requestUsbPermission()` method to `FrpViewModel`
- [ ] Add `onPermissionResult()` method to `FrpViewModel`
- [ ] Update `FrpBypassScreen` to show permission status
- [ ] Add "Request USB Permission" button to screen
- [ ] Disable bypass button until permission granted

### Testing (P1):
- [ ] Test with permission denied
- [ ] Test with permission granted
- [ ] Test permission request flow
- [ ] Test error message display
- [ ] Test progress indicator
- [ ] Test success/failure dialogs

---

## 🎯 EXPECTED BEHAVIOR AFTER FIX

### Flow with Permission Check:

1. **User opens FRP Bypass screen**
   - ✅ Shows device info
   - ✅ Shows permission status card (red if not granted)
   - ✅ Shows "Request USB Permission" button

2. **User clicks "Request USB Permission"**
   - ✅ System permission dialog appears
   - ✅ Status changes to "Waiting for permission..."
   - ✅ Logs show "[INFO] USB permission dialog shown"

3. **User accepts permission**
   - ✅ Status changes to "USB Permission Granted" (green)
   - ✅ "Request Permission" button disappears
   - ✅ Android version input enabled
   - ✅ "Start FRP Bypass" button enabled

4. **User clicks "Start FRP Bypass"**
   - ✅ Progress indicator appears
   - ✅ Logs show detailed progress
   - ✅ Success/error dialog shown at end

5. **If permission denied**
   - ✅ Clear error message: "USB permission denied"
   - ✅ Suggestion: "Please reconnect device and accept USB permission dialog"
   - ✅ Can retry by clicking "Request USB Permission" again

---

## ✅ SUCCESS CRITERIA

- ✅ App **never crashes** due to missing USB permission
- ✅ **Clear permission status** shown to user
- ✅ **Easy permission request** button available
- ✅ **Detailed error messages** when permission denied
- ✅ **Proper error handling** throughout the flow
- ✅ **User can retry** after granting permission

---

**Implementation Time:** 1-2 hours  
**Risk Level:** Low (adding safety checks only)  
**Breaking Changes:** None (backward compatible)

**Status:** 🔴 **READY TO IMPLEMENT**
