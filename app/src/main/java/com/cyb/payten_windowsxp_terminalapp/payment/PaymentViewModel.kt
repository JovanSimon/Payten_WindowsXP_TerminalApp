package com.cyb.payten_windowsxp_terminalapp.payment


import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyb.payten_windowsxp_terminalapp.auth.AuthData
import com.cyb.payten_windowsxp_terminalapp.auth.AuthStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject


@HiltViewModel
class PaymentViewModel @SuppressLint("StaticFieldLeak")
@Inject constructor(
    private val authStore: AuthStore,
//    private val getPackageManager: PackageManager
    private val context : Context
): ViewModel() {

    private val _state = MutableStateFlow(PaymentContract.PaymentContractUiState())
    val state = _state.asStateFlow()

//    private var paymentJson: RequestJson? = null  // Ovo će čuvati JSON u memoriji

    private fun setState(reducer: PaymentContract.PaymentContractUiState.() -> PaymentContract.PaymentContractUiState) {
        _state.update(reducer)
    }

    private val events = MutableSharedFlow<PaymentContract.PaymentContactUiEvent>()
    fun setEvent(event: PaymentContract.PaymentContactUiEvent) = viewModelScope.launch { events.emit(event) }

    init {
        populateState()
        observeTokens()
        observeClearDataStore()
        observePayButton()
    }

    private fun observeClearDataStore() {
        viewModelScope.launch {
            events
                .filterIsInstance<PaymentContract.PaymentContactUiEvent.ClearDataStore>()
                .collect {
                    authStore.updateAuthData(
                        AuthData(
                            user_id = -1,
                            first_name = "",
                            membership = "",
                            discount = 0f
                        )
                    )
                }
        }
    }


    private fun observePayButton() {
        viewModelScope.launch {
            events
                .filterIsInstance<PaymentContract.PaymentContactUiEvent.PayCLick>()
                .collect {
                    // Ovde treba da se pozove API za plaćanje
                    setState { copy(paymentJson = RequestJson(
                        header = Header(),
                        request = Request(
                            financial = Financial(
                                transaction = "sale",
                                id = Id(),
                                amounts = Amounts(base = it.value), // Početna vrednost base
                                options = Options()
                            )
                        )
                    ))}

                    sendJsonStringToApos("sale_request_example.json")
                    print("Payment JSON: ${state.value.paymentJson}")
                }
        }
    }

    private fun populateState() {
        val authData = authStore.authData.value

        if (authData.user_id != -1) {
            setState {
                copy(username = authData.first_name, membership = authData.membership, discount = authData.discount)
            }
            setState { copy(paymentJson = RequestJson(
                header = Header(),
                request = Request(
                    financial = Financial(
                        transaction = "sale",
                        id = Id(),
                        amounts = Amounts(base = "0.00"), // Početna vrednost base
                        options = Options()
                    )
                )
            ))}
        }
    }

    private fun observeTokens() {
        viewModelScope.launch {
            events
                .filterIsInstance<PaymentContract.PaymentContactUiEvent.ChangeTokenAmount>()
                .collect { event ->
                    if (event.value) {
                        setState { copy(token = state.value.token.inc()) }
                    } else {
                        if (state.value.token != 0) {
                            setState { copy(token = state.value.token.dec()) }
                        }
                    }

                    val time = state.value.token * 100
                    setState { copy(time = time) }

                    val basePrice = state.value.priceOfToken * state.value.token
                    val discount = basePrice * state.value.discount
                    val totalPrice = basePrice - discount

                    setState { copy(basePrice = basePrice, discountToShow = discount, totalPrice = totalPrice) }

                }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    @Throws(
        RemoteException::class,
        InterruptedException::class
    )
    private fun sendJsonStringToApos(json: String) {
//            val i = Intent("android.intent.action.MAIN")
//            i.setComponent(
//                ComponentName(
//                    "com.payten.paytenapos",
//                    "com.payten.paytenapos.ui.activities.SplashActivity"
//                )
//            )
//            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
//            if (i.resolveActivity(context.packageManager) != null) {
//                context.startActivity(i)
//                Log.e("TEST", "activity started")
//            }
//            Thread.sleep(500);
            val intent = Intent("com.payten.ecr.action")
            intent.setPackage("com.payten.paytenapos")
            intent.putExtra("ecrJson", getAssetJsonData(context,json))
            intent.putExtra("senderIntentFilter", "senderIntentFilter")
            intent.putExtra("senderPackage", context.packageName)
            intent.putExtra("senderClass", "com.cyb.payten_windowsxp_terminalapp.MainActivity");
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
            context.sendBroadcast(intent)
    }

    fun getAssetJsonData(context: Context, jsonString: String?): String? {
        var json: String? = null
        try {
            val `is` = context.assets.open(jsonString!!)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            json = String(buffer, charset("UTF-8"))
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

}

