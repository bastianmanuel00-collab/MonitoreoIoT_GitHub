package com.example.salaguardiot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.salaguardiot.ui.theme.SalaGuardIoTTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ==========================================================
// IMPORTANTE:
// REEMPLAZA SOLO EL TEXTO DE ABAJO POR TU READ API KEY
// NO ME ENVÍES TU API KEY.
// ==========================================================

private const val THINGSPEAK_CHANNEL_ID = "3550082"
private const val THINGSPEAK_READ_KEY = "TU_READ_API_KEY"


enum class Pantalla {
    INICIO,
    MONITOREO,
    DETALLE,
    DISPOSITIVO,
    ALERTAS,
    REPORTES,
    AJUSTES
}


data class DatosIoT(
    val temp1: Float = 25f,
    val temp2: Float = 27f,
    val temp3: Float = 33f,
    val temp4: Float = 26f,
    val humedad: Float = 48f,
    val humo: Int = 0,
    val ventilacion: Int = 1,
    val compuerta: Int = 0,
    val fecha: String = ""
)


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            SalaGuardIoTTheme {

                var sesionIniciada by remember {
                    mutableStateOf(false)
                }

                var pantallaActual by remember {
                    mutableStateOf(Pantalla.INICIO)
                }

                var datos by remember {
                    mutableStateOf(DatosIoT())
                }

                var cargando by remember {
                    mutableStateOf(false)
                }

                var mensajeConexion by remember {
                    mutableStateOf("Esperando conexión")
                }

                val scope = rememberCoroutineScope()

                fun actualizarThingSpeak() {

                    scope.launch {

                        cargando = true

                        val resultado =
                            leerDatosThingSpeak()

                        if (resultado != null) {

                            datos = resultado
                            mensajeConexion =
                                "Conectado a ThingSpeak"

                        } else {

                            mensajeConexion =
                                "No se pudo actualizar ThingSpeak"
                        }

                        cargando = false
                    }
                }

                LaunchedEffect(sesionIniciada) {

                    if (sesionIniciada) {
                        actualizarThingSpeak()
                    }
                }

                if (!sesionIniciada) {

                    LoginScreen(
                        onLoginSuccess = {
                            sesionIniciada = true
                        }
                    )

                } else {

                    when (pantallaActual) {

                        Pantalla.INICIO -> {

                            InicioScreen(
                                datos = datos,
                                mensajeConexion = mensajeConexion,
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }

                        Pantalla.MONITOREO -> {

                            MonitoreoScreen(
                                datos = datos,
                                cargando = cargando,
                                mensajeConexion = mensajeConexion,
                                onRefresh = {
                                    actualizarThingSpeak()
                                },
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }

                        Pantalla.DETALLE -> {

                            DetalleRackScreen(
                                datos = datos,
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }

                        Pantalla.DISPOSITIVO -> {

                            AgregarDispositivoScreen(
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }

                        Pantalla.ALERTAS -> {

                            AlertasScreen(
                                datos = datos,
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }

                        Pantalla.REPORTES -> {

                            ReportesScreen(
                                datos = datos,
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }

                        Pantalla.AJUSTES -> {

                            AjustesScreen(
                                onNavigate = {
                                    pantallaActual = it
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================================
// THINGSPEAK
// ==========================================================

suspend fun leerDatosThingSpeak(): DatosIoT? {

    return withContext(Dispatchers.IO) {

        try {

            if (
                THINGSPEAK_READ_KEY ==
                "TU_READ_API_KEY"
            ) {
                return@withContext null
            }

            val url =
                "https://api.thingspeak.com/channels/" +
                        "$THINGSPEAK_CHANNEL_ID/feeds/last.json" +
                        "?api_key=$THINGSPEAK_READ_KEY"

            val respuesta =
                URL(url).readText()

            val json =
                JSONObject(respuesta)

            DatosIoT(

                temp1 =
                    json.optString("field1")
                        .toFloatOrNull() ?: 25f,

                temp2 =
                    json.optString("field2")
                        .toFloatOrNull() ?: 27f,

                temp3 =
                    json.optString("field3")
                        .toFloatOrNull() ?: 33f,

                temp4 =
                    json.optString("field4")
                        .toFloatOrNull() ?: 26f,

                humedad =
                    json.optString("field5")
                        .toFloatOrNull() ?: 48f,

                humo =
                    json.optString("field6")
                        .toIntOrNull() ?: 0,

                ventilacion =
                    json.optString("field7")
                        .toIntOrNull() ?: 1,

                compuerta =
                    json.optString("field8")
                        .toIntOrNull() ?: 0,

                fecha =
                    json.optString("created_at")
            )

        } catch (e: Exception) {

            null
        }
    }
}


// ==========================================================
// FUNCIONES DE ESTADO
// ==========================================================

fun estadoRack(
    temperatura: Float,
    humo: Int = 0
): String {

    return when {

        humo == 1 ->
            "PELIGRO"

        temperatura >= 35 ->
            "PELIGRO"

        temperatura >= 30 ->
            "PRECAUCIÓN"

        else ->
            "ESTABLE"
    }
}


fun colorEstado(
    estado: String
): Color {

    return when (estado) {

        "PELIGRO" ->
            Color(0xFFB91C1C)

        "PRECAUCIÓN" ->
            Color(0xFF9A6700)

        else ->
            Color(0xFF198754)
    }
}


fun fondoEstado(
    estado: String
): Color {

    return when (estado) {

        "PELIGRO" ->
            Color(0xFFFFE4E4)

        "PRECAUCIÓN" ->
            Color(0xFFFFF1C2)

        else ->
            Color(0xFFE8F7EE)
    }
}


fun temperaturaTexto(
    valor: Float
): String {

    return if (
        valor == valor.toInt().toFloat()
    ) {

        "${valor.toInt()} °C"

    } else {

        "$valor °C"
    }
}


// ==========================================================
// LOGIN
// ==========================================================

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {

    val context =
        LocalContext.current

    val db =
        remember {
            DatabaseHelper(context)
        }

    var usuario by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FA))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = "Monitoreo IoT",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "Monitoreo y protección de una sala de servidores",
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )

            Text(
                text = "Iniciar sesión",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 22.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Usuario")
                },
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Contraseña")
                },
                visualTransformation =
                    PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(
                modifier = Modifier.height(22.dp)
            )

            Button(
                onClick = {

                    if (
                        db.validarUsuario(
                            usuario,
                            password
                        )
                    ) {

                        mensaje = ""
                        onLoginSuccess()

                    } else {

                        mensaje =
                            "Usuario o contraseña incorrectos."
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF2F66D0)
                    )
            ) {

                Text("Ingresar")
            }

            if (
                mensaje.isNotEmpty()
            ) {

                Spacer(
                    modifier =
                        Modifier.height(14.dp)
                )

                Text(
                    mensaje,
                    color =
                        MaterialTheme.colorScheme.error
                )
            }

            Spacer(
                modifier = Modifier.height(32.dp)
            )

            Text(
                text = "Sala 300 · Sistema IoT",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}


// ==========================================================
// 01 INICIO
// ==========================================================

@Composable
fun InicioScreen(
    datos: DatosIoT,
    mensajeConexion: String,
    onNavigate: (Pantalla) -> Unit
) {

    val estado =
        estadoRack(
            datos.temp3,
            datos.humo
        )

    PantallaBase(
        pantalla = Pantalla.INICIO,
        onNavigate = onNavigate
    ) {

        Text(
            "Monitoreo de sala de servidores",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Text(
            "Monitoreo IoT",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "WiFi · Sala 300 — $mensajeConexion",
            fontSize = 13.sp,
            color = Color(0xFF64748B)
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color(0xFF173A63)
                ),
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(18.dp)
            ) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        "Sala 300",
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    EstadoBadge(estado)
                }

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "Rack 03 requiere atención · ${
                        temperaturaTexto(datos.temp3)
                    }",
                    color =
                        Color(0xFFD6DFEA),
                    fontSize = 13.sp
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Text(
                    "Rack 01     Rack 02     Rack 03     Rack 04",
                    color =
                        Color(0xFFD6DFEA),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "Menú principal",
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            MenuCard(
                "Monitoreo",
                "Lecturas por rack",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    Pantalla.MONITOREO
                )
            }

            MenuCard(
                "Alertas",
                "Estado del sistema",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    Pantalla.ALERTAS
                )
            }
        }

        Spacer(
            Modifier.height(10.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            MenuCard(
                "Dispositivos",
                "Agregar por WiFi",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    Pantalla.DISPOSITIVO
                )
            }

            MenuCard(
                "Reportes",
                "Historial 24 h",
                Modifier.weight(1f)
            ) {
                onNavigate(
                    Pantalla.REPORTES
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        fondoEstado(estado)
                ),
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(15.dp)
            ) {

                Text(
                    when (estado) {

                        "PELIGRO" ->
                            "⚠ Peligro detectado"

                        "PRECAUCIÓN" ->
                            "⚠ Ventilación automática activada"

                        else ->
                            "✓ Sistema estable"
                    },
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        colorEstado(estado)
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    when (estado) {

                        "PELIGRO" ->
                            "Se detectó una condición de peligro en Rack 03."

                        "PRECAUCIÓN" ->
                            "Temperatura elevada en Rack 03."

                        else ->
                            "Todos los valores se encuentran dentro del rango normal."
                    },
                    fontSize = 12.sp
                )
            }
        }
    }
}


// ==========================================================
// 02 MONITOREO
// ==========================================================

@Composable
fun MonitoreoScreen(
    datos: DatosIoT,
    cargando: Boolean,
    mensajeConexion: String,
    onRefresh: () -> Unit,
    onNavigate: (Pantalla) -> Unit
) {

    PantallaBase(
        pantalla = Pantalla.MONITOREO,
        onNavigate = onNavigate
    ) {

        Text(
            "SALA 300 · VISTA GENERAL",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Text(
            "Monitoreo",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Text(
            mensajeConexion,
            color = Color(0xFF198754),
            fontSize = 13.sp
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Button(
            onClick = onRefresh,
            enabled = !cargando,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                if (cargando)
                    "Actualizando..."
                else
                    "Actualizar desde ThingSpeak"
            )
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Text(
            "Temperatura por rack",
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            RackCard(
                "Rack 01",
                datos.temp1,
                Modifier.weight(1f)
            )

            RackCard(
                "Rack 02",
                datos.temp2,
                Modifier.weight(1f)
            )
        }

        Spacer(
            Modifier.height(10.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            RackCard(
                "Rack 03",
                datos.temp3,
                Modifier.weight(1f),
                onClick = {
                    onNavigate(
                        Pantalla.DETALLE
                    )
                }
            )

            RackCard(
                "Rack 04",
                datos.temp4,
                Modifier.weight(1f)
            )
        }

        Spacer(
            Modifier.height(22.dp)
        )

        Text(
            "Protección automática · Rack 03",
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(10.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(16.dp)
            ) {

                EstadoFila(
                    "Ventilación",
                    if (
                        datos.ventilacion == 1
                    )
                        "ON · Activada"
                    else
                        "OFF"
                )

                HorizontalDivider()

                EstadoFila(
                    "Humo/gas",
                    if (
                        datos.humo == 1
                    )
                        "Detectado"
                    else
                        "No detectado"
                )

                HorizontalDivider()

                EstadoFila(
                    "Compuerta",
                    if (
                        datos.compuerta == 1
                    )
                        "Cerrada"
                    else
                        "Abierta"
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        Text(
            "Presiona Rack 03 para ver su detalle.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier =
                Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}


// ==========================================================
// 03 DETALLE RACK
// ==========================================================

@Composable
fun DetalleRackScreen(
    datos: DatosIoT,
    onNavigate: (Pantalla) -> Unit
) {

    val estado =
        estadoRack(
            datos.temp3,
            datos.humo
        )

    PantallaBase(
        pantalla = Pantalla.MONITOREO,
        onNavigate = onNavigate
    ) {

        Text(
            "Monitoreo",
            color = Color.Gray
        )

        Text(
            "Detalle del rack",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Sala 300 — Rack 03",
            color = Color.Gray
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(18.dp)
            ) {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Text(
                        "Rack 03",
                        fontSize = 20.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    EstadoBadge(estado)
                }

                Spacer(
                    Modifier.height(16.dp)
                )

                Row(
                    Modifier.fillMaxWidth()
                ) {

                    Column(
                        Modifier.weight(1f)
                    ) {

                        Text(
                            "Temperatura",
                            color = Color.Gray
                        )

                        Text(
                            temperaturaTexto(
                                datos.temp3
                            ),
                            fontSize = 28.sp,
                            fontWeight =
                                FontWeight.Bold
                        )
                    }

                    Column(
                        Modifier.weight(1f)
                    ) {

                        Text(
                            "Humedad",
                            color = Color.Gray
                        )

                        Text(
                            "${datos.humedad.toInt()} %",
                            fontSize = 28.sp,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Text(
                            "Sensor DHT22",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        fondoEstado(estado)
                ),
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(15.dp)
            ) {

                Text(
                    when (estado) {

                        "PELIGRO" ->
                            "Condición de peligro"

                        "PRECAUCIÓN" ->
                            "Condición preventiva"

                        else ->
                            "Condición estable"
                    },
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        colorEstado(estado)
                )

                Text(
                    when (estado) {

                        "PELIGRO" ->
                            "Se requiere atención inmediata."

                        "PRECAUCIÓN" ->
                            "Temperatura elevada. Ventilación automática activada."

                        else ->
                            "Los sensores se encuentran en rango normal."
                    },
                    fontSize = 12.sp
                )
            }
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Sensores y protección",
            fontWeight =
                FontWeight.SemiBold
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(16.dp)
            ) {

                EstadoFila(
                    "Humo/gas",
                    if (
                        datos.humo == 1
                    )
                        "Detectado"
                    else
                        "No detectado"
                )

                HorizontalDivider()

                EstadoFila(
                    "Ventilación",
                    if (
                        datos.ventilacion == 1
                    )
                        "ON"
                    else
                        "OFF"
                )

                HorizontalDivider()

                EstadoFila(
                    "Compuerta",
                    if (
                        datos.compuerta == 1
                    )
                        "Cerrada"
                    else
                        "Abierta"
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        Button(
            onClick = {
                onNavigate(
                    Pantalla.REPORTES
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Ver historial del rack"
            )
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Text(
            "ESP32 / WiFi · DHT22 + MQ-2",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier =
                Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}


// ==========================================================
// 04 AGREGAR DISPOSITIVO
// ==========================================================

@Composable
fun AgregarDispositivoScreen(
    onNavigate: (Pantalla) -> Unit
) {

    var nombre by remember {
        mutableStateOf("Sensor Rack 05")
    }

    var rack by remember {
        mutableStateOf("Rack 05")
    }

    var wifi by remember {
        mutableStateOf("Sala300-IoT")
    }

    var mensaje by remember {
        mutableStateOf("")
    }

    PantallaBase(
        pantalla = Pantalla.INICIO,
        onNavigate = onNavigate
    ) {

        Text(
            "Agregar dispositivo",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Dispositivos · Sala 300",
            color = Color.Gray
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Vincula un dispositivo IoT",
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(12.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
            },
            label = {
                Text(
                    "Nombre del dispositivo"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(10.dp)
        )

        OutlinedTextField(
            value =
                "Temperatura / humedad · DHT22",
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    "Tipo de dispositivo"
                )
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = rack,
            onValueChange = {
                rack = it
            },
            label = {
                Text("Rack / ubicación")
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(10.dp)
        )

        OutlinedTextField(
            value = wifi,
            onValueChange = {
                wifi = it
            },
            label = {
                Text("Red WiFi")
            },
            modifier =
                Modifier.fillMaxWidth()
        )

        Spacer(
            Modifier.height(15.dp)
        )

        Button(
            onClick = {
                mensaje =
                    "Dispositivo vinculado correctamente."
            },
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                "Vincular dispositivo"
            )
        }

        if (
            mensaje.isNotEmpty()
        ) {

            Spacer(
                Modifier.height(10.dp)
            )

            Text(
                mensaje,
                color =
                    Color(0xFF198754)
            )
        }
    }
}


// ==========================================================
// 05 ALERTAS
// ==========================================================

@Composable
fun AlertasScreen(
    datos: DatosIoT,
    onNavigate: (Pantalla) -> Unit
) {

    val estado =
        estadoRack(
            datos.temp3,
            datos.humo
        )

    PantallaBase(
        pantalla = Pantalla.ALERTAS,
        onNavigate = onNavigate
    ) {

        Text(
            "SALA 300 · NOTIFICACIONES",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Text(
            "Alertas",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Todas · 3      Activas · 1      Historial · 2",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(
            Modifier.height(14.dp)
        )

        AlertaCard(
            titulo = estado,
            detalle =
                "Sala 300 — Rack 03 — ${
                    temperaturaTexto(datos.temp3)
                }",
            descripcion =
                if (estado == "PELIGRO")
                    "Condición peligrosa detectada."
                else
                    "Temperatura elevada. Ventilación automática activada.",
            activa = true
        )

        Spacer(
            Modifier.height(12.dp)
        )

        AlertaCard(
            titulo = "PELIGRO",
            detalle =
                "Sala 300 — Rack 02",
            descripcion =
                "Humo detectado. Evento resuelto.",
            activa = false
        )

        Spacer(
            Modifier.height(12.dp)
        )

        AlertaCard(
            titulo = "ESTABLE",
            detalle =
                "Sala 300 — Rack 01 — ${temperaturaTexto(datos.temp1)}",
            descripcion =
                "Temperatura normal y sin humo.",
            activa = false
        )
    }
}


// ==========================================================
// 06 REPORTES
// ==========================================================

@Composable
fun ReportesScreen(
    datos: DatosIoT,
    onNavigate: (Pantalla) -> Unit
) {

    PantallaBase(
        pantalla = Pantalla.INICIO,
        onNavigate = onNavigate
    ) {

        Text(
            "Reportes e historial",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Sala 300 — Rack 03",
            color = Color.Gray
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(18.dp)
            ) {

                Text(
                    "Temperatura · Rack 03",
                    fontWeight =
                        FontWeight.SemiBold
                )

                Spacer(
                    Modifier.height(18.dp)
                )

                Text(
                    "33°     ─────────────●",
                    fontSize = 18.sp
                )

                Text(
                    "30°     ────────●",
                    fontSize = 18.sp
                )

                Text(
                    "27°     ────●",
                    fontSize = 18.sp
                )

                Text(
                    "24°     ●",
                    fontSize = 18.sp
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "Últimas 24 horas",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(
            Modifier.height(14.dp)
        )

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            EstadisticaCard(
                "Máxima",
                maxOf(
                    33f,
                    datos.temp3
                ),
                Modifier.weight(1f)
            )

            EstadisticaCard(
                "Mínima",
                24f,
                Modifier.weight(1f)
            )

            EstadisticaCard(
                "Promedio",
                27.8f,
                Modifier.weight(1f)
            )
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Eventos del rack",
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            Modifier.height(8.dp)
        )

        EstadoFila(
            "Lectura actual",
            temperaturaTexto(
                datos.temp3
            )
        )

        HorizontalDivider()

        EstadoFila(
            "Evento anterior",
            "30 °C · Precaución"
        )

        HorizontalDivider()

        EstadoFila(
            "Evento estable",
            "28 °C"
        )
    }
}


// ==========================================================
// 07 AJUSTES
// ==========================================================

@Composable
fun AjustesScreen(
    onNavigate: (Pantalla) -> Unit
) {

    var notificacionPrecaucion by remember {
        mutableStateOf(true)
    }

    var notificacionPeligro by remember {
        mutableStateOf(true)
    }

    var desconexion by remember {
        mutableStateOf(true)
    }

    PantallaBase(
        pantalla = Pantalla.AJUSTES,
        onNavigate = onNavigate
    ) {

        Text(
            "SALA 300 · AJUSTES DEL SISTEMA",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Text(
            "Configuración",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Umbrales de temperatura",
            fontWeight =
                FontWeight.SemiBold
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(16.dp)
            ) {

                EstadoFila(
                    "Umbral de precaución",
                    "30 °C"
                )

                HorizontalDivider()

                EstadoFila(
                    "Umbral de peligro",
                    "35 °C"
                )
            }
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Notificaciones",
            fontWeight =
                FontWeight.SemiBold
        )

        SwitchFila(
            "Notificaciones de precaución",
            notificacionPrecaucion
        ) {
            notificacionPrecaucion = it
        }

        SwitchFila(
            "Notificaciones de peligro",
            notificacionPeligro
        ) {
            notificacionPeligro = it
        }

        SwitchFila(
            "Avisos de desconexión",
            desconexion
        ) {
            desconexion = it
        }

        Spacer(
            Modifier.height(18.dp)
        )

        Text(
            "Aplicación y conexión",
            fontWeight =
                FontWeight.SemiBold
        )

        Card(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Column(
                Modifier.padding(16.dp)
            ) {

                EstadoFila(
                    "Unidad",
                    "°C"
                )

                HorizontalDivider()

                EstadoFila(
                    "Tema",
                    "Claro"
                )

                HorizontalDivider()

                EstadoFila(
                    "Sala",
                    "Sala 300"
                )

                HorizontalDivider()

                EstadoFila(
                    "Conexión",
                    "WiFi"
                )
            }
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "El humo detectado activa PELIGRO independientemente de la temperatura.",
            fontSize = 12.sp,
            color = Color(0xFFB91C1C)
        )
    }
}


// ==========================================================
// COMPONENTES
// ==========================================================

@Composable
fun PantallaBase(
    pantalla: Pantalla,
    onNavigate: (Pantalla) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {

    Scaffold(
        containerColor =
            Color(0xFFF4F6FA),
        bottomBar = {

            BottomNavigation(
                pantalla = pantalla,
                onNavigate = onNavigate
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(20.dp),
            content = content
        )
    }
}


@Composable
fun BottomNavigation(
    pantalla: Pantalla,
    onNavigate: (Pantalla) -> Unit
) {

    Surface(
        color = Color.White
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            horizontalArrangement =
                Arrangement.SpaceEvenly,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            BottomNavItem(
                "⌂",
                "Inicio",
                pantalla ==
                        Pantalla.INICIO
            ) {
                onNavigate(
                    Pantalla.INICIO
                )
            }

            BottomNavItem(
                "◉",
                "Monitoreo",
                pantalla ==
                        Pantalla.MONITOREO
            ) {
                onNavigate(
                    Pantalla.MONITOREO
                )
            }

            BottomNavItem(
                "!",
                "Alertas",
                pantalla ==
                        Pantalla.ALERTAS
            ) {
                onNavigate(
                    Pantalla.ALERTAS
                )
            }

            BottomNavItem(
                "☷",
                "Ajustes",
                pantalla ==
                        Pantalla.AJUSTES
            ) {
                onNavigate(
                    Pantalla.AJUSTES
                )
            }
        }
    }
}


@Composable
fun BottomNavItem(
    icono: String,
    texto: String,
    activo: Boolean,
    onClick: () -> Unit
) {

    Column(
        modifier = Modifier
            .clickable {
                onClick()
            }
            .padding(8.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            icono,
            fontSize = 19.sp,
            color =
                if (activo)
                    Color(0xFF2F66D0)
                else
                    Color.Gray
        )

        Text(
            texto,
            fontSize = 10.sp,
            color =
                if (activo)
                    Color(0xFF2F66D0)
                else
                    Color.Gray
        )
    }
}


@Composable
fun MenuCard(
    titulo: String,
    subtitulo: String,
    modifier: Modifier,
    onClick: () -> Unit
) {

    Card(
        modifier = modifier
            .height(105.dp)
            .clickable {
                onClick()
            }
    ) {

        Column(
            Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                "↗",
                color =
                    Color(0xFF2F66D0)
            )

            Spacer(
                Modifier.weight(1f)
            )

            Text(
                titulo,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                subtitulo,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}


@Composable
fun RackCard(
    nombre: String,
    temperatura: Float,
    modifier: Modifier,
    onClick: () -> Unit = {}
) {

    val estado =
        estadoRack(temperatura)

    Card(
        modifier = modifier
            .height(135.dp)
            .clickable {
                onClick()
            },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (
                        estado ==
                        "PRECAUCIÓN"
                    )
                        Color(0xFFFFF7E2)
                    else
                        Color.White
            )
    ) {

        Column(
            Modifier.padding(14.dp)
        ) {

            Text(nombre)

            Spacer(
                Modifier.height(14.dp)
            )

            Text(
                temperaturaTexto(
                    temperatura
                ),
                fontSize = 27.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                Modifier.height(8.dp)
            )

            EstadoBadge(estado)
        }
    }
}


@Composable
fun EstadoBadge(
    estado: String
) {

    Surface(
        color =
            fondoEstado(estado),
        shape =
            RoundedCornerShape(12.dp)
    ) {

        Text(
            "● $estado",
            modifier =
                Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                ),
            color =
                colorEstado(estado),
            fontSize = 10.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
fun EstadoFila(
    nombre: String,
    valor: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 10.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            nombre,
            color = Color.Gray
        )

        Text(
            valor,
            fontWeight =
                FontWeight.SemiBold
        )
    }
}


@Composable
fun AlertaCard(
    titulo: String,
    detalle: String,
    descripcion: String,
    activa: Boolean
) {

    Card(
        modifier =
            Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    fondoEstado(titulo)
            )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ) {

                Text(
                    titulo,
                    color =
                        colorEstado(titulo),
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    if (activa)
                        "ACTIVA"
                    else
                        "REGISTRO",
                    fontSize = 10.sp
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                detalle,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                descripcion,
                fontSize = 12.sp
            )
        }
    }
}


@Composable
fun EstadisticaCard(
    titulo: String,
    valor: Float,
    modifier: Modifier
) {

    Card(
        modifier = modifier
    ) {

        Column(
            Modifier.padding(12.dp)
        ) {

            Text(
                titulo,
                fontSize = 11.sp,
                color = Color.Gray
            )

            Text(
                if (
                    titulo ==
                    "Promedio"
                )
                    "$valor °C"
                else
                    temperaturaTexto(valor),
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}


@Composable
fun SwitchFila(
    texto: String,
    checked: Boolean,
    onCheckedChange:
        (Boolean) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 5.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            texto,
            modifier =
                Modifier.weight(1f)
        )

        Switch(
            checked = checked,
            onCheckedChange =
                onCheckedChange
        )
    }
}