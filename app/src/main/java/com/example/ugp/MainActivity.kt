package com.example.ugp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.system.measureNanoTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF1E9E54),
                    onPrimary = Color.White,
                    surface = Color.White,
                    onSurface = Color(0xFF111111)
                )
            ) {
                RideScreen()
            }
        }
    }
}

/* ----------------------------- Data & Helpers ----------------------------- */

data class RideRequest(
    val id: Int,
    val pickup: String,
    val drop: String,
    val distanceKm: Double,
    val fare: Int,
    val passengerName: String,
    val rating: Double,
    val etaToPickupMin: Int,
    val tripTimeMin: Int,
    val adminClickTimestamp: String = ""
)

data class TimestampRecord(
    val rideId: Int,
    val adminClickTime: String,
    val driverReceiveTime: String = "",
    val driverAcceptTime: String = ""
)

// Pre-made ride combinations for admin
private val preMadeRides = listOf(
    Triple("Z Square Mall", "Kanpur Central", 4.2),
    Triple("Nana Rao Park", "JK Temple", 6.8),
    Triple("Green Park Stadium", "IIT Kanpur", 8.5),
    Triple("Moti Jheel", "Allen Forest Zoo", 5.3),
    Triple("Ghanta Ghar", "Rave 3 Mall", 7.1)
)

private val locations = listOf(
    "Z Square Mall", "Nana Rao Park", "Kanpur Central", "Green Park Stadium",
    "JK Temple", "Moti Jheel", "Allen Forest Zoo", "IIT Kanpur",
    "Ghanta Ghar", "Rave 3 Mall"
)

private fun allPairs() =
    locations.flatMapIndexed { i, a -> locations.drop(i + 1).map { b -> a to b } }

/** Deterministic pseudo address for a location name (so preview looks real). */
private fun detailedAddressFor(name: String): String {
    val areas = listOf(
        "Arya Nagar", "Swaroop Nagar", "Civil Lines", "Kakadeo", "Kalyanpur",
        "Shastri Nagar", "Govind Nagar", "Panki", "Jajmau", "Nawabganj"
    )
    return when (name) {
        "Z Square Mall" -> "Arya Nagar, Mall Road, Kanpur"
        "Nana Rao Park" -> "Shastri Nagar, Park Area, Kanpur"
        "Kanpur Central" -> "Civil Lines, Railway Station, Kanpur"
        "Green Park Stadium" -> "Kalyanpur, Stadium Road, Kanpur"
        "JK Temple" -> "Govind Nagar, Temple Road, Kanpur"
        "Moti Jheel" -> "Kakadeo, Lake Area, Kanpur"
        "Allen Forest Zoo" -> "Panki, Zoo Road, Kanpur"
        "IIT Kanpur" -> "Kalyanpur, IIT Campus, Kanpur"
        "Ghanta Ghar" -> "Jajmau, City Center, Kanpur"
        "Rave 3 Mall" -> "Nawabganj, Mall Road, Kanpur"
        else -> "${areas[name.hashCode() % areas.size]}, Kanpur"
    }
}

/* --------------------------------- Screen --------------------------------- */

@Composable
fun RideScreen() {
    var current by remember { mutableStateOf<RideRequest?>(null) }
    val history = remember { mutableStateListOf<RideRequest>() }
    val pairs = remember { allPairs().shuffled().toMutableList() }
    var lastId by remember { mutableStateOf(0) }

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Admin mode state
    var isAdminMode by remember { mutableStateOf(false) }
    var autoGenerateRides by remember { mutableStateOf(true) }
    var preMadeRideIndex by remember { mutableStateOf(0) }
    val timestampRecords = remember { mutableStateListOf<TimestampRecord>() }
    
    val context = LocalContext.current
    fun playNotificationSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            toneGenerator.release()
        } catch (e: Exception) { }
    }
    
    fun playRideEndSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            toneGenerator.release()
        } catch (e: Exception) { }
    }

    // Auto-generate rides when not in admin mode
    LaunchedEffect(current, isAdminMode, autoGenerateRides) {
        if (!isAdminMode && current == null && pairs.isNotEmpty() && autoGenerateRides) {
            delay(Random.nextLong(1800, 3500))
            val (p, d) = pairs.removeAt(0)
            val dist = Random.nextDouble(2.5, 6.0)
            val etaPickup = Random.nextInt(1, 4)
            val tripTime = (dist * 3.0).roundToInt() + 12
            val rideId = ++lastId
            current = RideRequest(
                id = rideId,
                pickup = p,
                drop = d,
                distanceKm = dist,
                fare = 40 + (dist * 22).toInt(),
                passengerName = listOf("Rajan", "Ananya", "Kabir", "Sanya", "Aditya").random(),
                rating = Random.nextDouble(4.2, 5.0),
                etaToPickupMin = etaPickup,
                tripTimeMin = tripTime
            )
            playNotificationSound()
        }
    }

    // Dynamic map based on ride ID
    val currentMapImage = when (current?.id) {
        1 -> R.drawable.map_ride_1
        2 -> R.drawable.map_ride_2
        3 -> R.drawable.map_ride_3
        4 -> R.drawable.map_ride_4
        else -> R.drawable.map_ride_5
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0xFF1E9E54))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (!isAdminMode) isAdminMode = true }) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Admin",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (isAdminMode) "ADMIN MODE" else "DRIVER MODE",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { /* Show history */ }) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "History",
                                tint = Color.White
                            )
                        }
                        if (isAdminMode) {
                            IconButton(onClick = { 
                                isAdminMode = false
                                scope.launch { snack.showSnackbar("Switched to Driver Mode") }
                            }) {
                                Text("DRIVER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // Map background
            Image(
                painter = painterResource(id = currentMapImage),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (isAdminMode) {
                AdminPanel(
                    current = current,
                    history = history,
                    preMadeRideIndex = preMadeRideIndex,
                    timestampRecords = timestampRecords,
                    onGeneratePreMadeRide = { index ->
                        val clickTime = measureNanoTime { }
                        val currentTime = System.currentTimeMillis()
                        val indianTime = currentTime + (5 * 60 * 60 * 1000) + (30 * 60 * 1000)
                        val hours = (indianTime / (1000 * 60 * 60)) % 24
                        val minutes = (indianTime / (1000 * 60)) % 60
                        val seconds = (indianTime / 1000) % 60
                        val microseconds = (clickTime % 1_000_000) / 1000
                        val timestamp = String.format("%02d:%02d:%02d.%06d", hours, minutes, seconds, microseconds)
                        
                        val (pickup, drop, distance) = preMadeRides[index]
                        val fare = 40 + (distance * 22).toInt()
                        val etaPickup = Random.nextInt(1, 4)
                        val tripTime = (distance * 3.0).roundToInt() + 12
                        val rideId = ++lastId
                        
                        current = RideRequest(
                            id = rideId,
                            pickup = pickup,
                            drop = drop,
                            distanceKm = distance,
                            fare = fare,
                            passengerName = listOf("Rajan", "Ananya", "Kabir", "Sanya", "Aditya").random(),
                            rating = Random.nextDouble(4.2, 5.0),
                            etaToPickupMin = etaPickup,
                            tripTimeMin = tripTime,
                            adminClickTimestamp = timestamp
                        )
                        
                        timestampRecords.add(TimestampRecord(rideId = rideId, adminClickTime = timestamp))
                        preMadeRideIndex = (preMadeRideIndex + 1) % preMadeRides.size
                        playNotificationSound()
                        scope.launch { snack.showSnackbar("Pre-made ride #${index + 1} sent at $timestamp") }
                    },
                    onCancelRide = {
                        current = null
                        scope.launch { snack.showSnackbar("Ride cancelled") }
                    },
                    autoGenerateRides = autoGenerateRides,
                    onToggleAutoGenerate = { autoGenerateRides = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            } else {
                // Driver mode - UGP-2 original layout
                current?.let { req ->
                    RideRequestAcceptPage(
                        request = req,
                        mapImageRes = currentMapImage,
                        onAccept = {
                            // Record timestamp
                            val currentTime = System.currentTimeMillis()
                            val indianTime = currentTime + (5 * 60 * 60 * 1000) + (30 * 60 * 1000)
                            val hours = (indianTime / (1000 * 60 * 60)) % 24
                            val minutes = (indianTime / (1000 * 60)) % 60
                            val seconds = (indianTime / 1000) % 60
                            val acceptTimestamp = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                            
                            val record = timestampRecords.find { it.rideId == req.id }
                            record?.let {
                                timestampRecords.remove(it)
                                timestampRecords.add(it.copy(driverAcceptTime = acceptTimestamp))
                            }
                            
                            history.add(req)
                            current = null
                            playRideEndSound()
                            scope.launch { snack.showSnackbar("Ride accepted at $acceptTimestamp") }
                        },
                        onOffer = { offerFare ->
                            history.add(req.copy(fare = offerFare))
                            current = null
                            playRideEndSound()
                        },
                        onClose = {
                            val currentTime = System.currentTimeMillis()
                            val indianTime = currentTime + (5 * 60 * 60 * 1000) + (30 * 60 * 1000)
                            val hours = (indianTime / (1000 * 60 * 60)) % 24
                            val minutes = (indianTime / (1000 * 60)) % 60
                            val seconds = (indianTime / 1000) % 60
                            val rejectTimestamp = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                            
                            val record = timestampRecords.find { it.rideId == req.id }
                            record?.let {
                                timestampRecords.remove(it)
                                timestampRecords.add(it.copy(driverAcceptTime = "REJECTED:$rejectTimestamp"))
                            }
                            
                            history.add(req)
                            current = null
                            playRideEndSound()
                            scope.launch { snack.showSnackbar("Request rejected at $rejectTimestamp") }
                        }
                    )
                } ?: EmptyState()
            }
        }
    }
}

/* ----------------------------- Empty State ----------------------------- */

/* ----------------------------- Empty State ----------------------------- */

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Waiting for ride requests...",
                fontSize = 18.sp,
                color = Color.White,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/* ----------------------------- Ride Request Page ----------------------------- */

/* ----------------------------- Ride Request Page ----------------------------- */

@Composable
fun RideRequestAcceptPage(
    request: RideRequest,
    mapImageRes: Int,
    onAccept: () -> Unit,
    onOffer: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topDeepGrey = Color(0xFF3B434A)
    val bottomDarkGrey = Color(0xFF1C1F23)
    val midGreyChip = Color(0xFF5C6770)

    val pickupDetail = remember(request.id) { detailedAddressFor(request.pickup) }
    val dropDetail = remember(request.id) { detailedAddressFor(request.drop) }
    val offerOptions = remember(request.fare) { threeOfferOptions(request.fare) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Map
        Image(
            painter = painterResource(id = mapImageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Header
        Text(
            text = "Ride request",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
        )

        // 3. Zoom Buttons
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundGreyIconButton(text = "+") { }
            RoundGreyIconButton(text = "–") { }
        }

        // 4. Bottom Card
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Surface(
                onClick = onClose,
                color = midGreyChip,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp).size(width = 85.dp, height = 32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Close", color = Color.White, fontSize = 13.sp)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = topDeepGrey,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF455A64)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, null, tint = Color.White)
                            }
                            Text(request.passengerName, color = Color.White, fontSize = 13.sp)

                            // ✅ YAHAN CHANGE KIYA HAI: Rating ko format kiya hai
                            Text(
                                text = "⭐ ${"%.1f".format(request.rating)}",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            LocationLineOnDark(label = "•", labelColor = Color.Green, title = request.pickup, subtitle = pickupDetail)
                            Spacer(Modifier.height(8.dp))
                            LocationLineOnDark(label = "•", labelColor = Color.Red, title = request.drop, subtitle = dropDetail)
                            Text("₹${request.fare}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Surface(color = bottomDarkGrey, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                offerOptions.forEach { amount ->
                                    Box(
                                        modifier = Modifier.weight(1f).background(topDeepGrey, RoundedCornerShape(8.dp)).clickable { onOffer(amount) }.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("₹$amount", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onAccept,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7FFF00)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("ACCEPT", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ✅ Is function ko sirf ek baar rakhein (Duplicate delete kar dein)
@Composable
fun RoundGreyIconButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0x99000000),
        shape = CircleShape,
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LocationLineOnDark(label: String, labelColor: Color, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(text = label, color = labelColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
    }
}

fun threeOfferOptions(baseFare: Int): List<Int> {
    return listOf(baseFare + 10, baseFare + 20, baseFare + 30)
}

@Composable
fun AdminPanel(
    current: RideRequest?,
    history: List<RideRequest>,
    preMadeRideIndex: Int,
    timestampRecords: List<TimestampRecord>,
    onGeneratePreMadeRide: (Int) -> Unit,
    onCancelRide: () -> Unit,
    autoGenerateRides: Boolean,
    onToggleAutoGenerate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text("Admin Controls", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { onGeneratePreMadeRide(preMadeRideIndex) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Pre-made Ride #${preMadeRideIndex + 1}")
            }
            if (current != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onCancelRide,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Current Ride")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoGenerateRides, onCheckedChange = onToggleAutoGenerate)
                Text("Auto-generate rides", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}