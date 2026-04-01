package com.simats.endecryption

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.android.billingclient.api.*
import com.simats.endecryption.databinding.ActivitySubscriptionBinding

class SubscriptionActivity : BaseActivity(), PurchasesUpdatedListener {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var billingClient: BillingClient
    private var productDetails: ProductDetails? = null

    companion object {
        private const val TAG = "SubscriptionActivity"
        private const val SUBSCRIPTION_SKU = "endecryption_premium_subscription"
        private const val TEST_SUBSCRIPTION_SKU = "android.test.purchased" // For testing
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        addDebugInformation()
        setupBillingClient()
        setupClickListeners()
    }

    private fun addDebugInformation() {
        Log.d(TAG, "=== DEBUG INFORMATION ===")
        Log.d(TAG, "Package name: ${packageName}")

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Log.d(TAG, "Version code: ${packageInfo.longVersionCode}")
            Log.d(TAG, "Version name: ${packageInfo.versionName}")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get package info: ${e.message}")
        }

        Log.d(TAG, "Product ID: $SUBSCRIPTION_SKU")
        Log.d(TAG, "Test Product ID: $TEST_SUBSCRIPTION_SKU")
        Log.d(TAG, "=========================")
    }

    private fun setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing setup finished successfully")
                    querySubscriptionDetails()
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing service disconnected")
            }
        })
    }

    private fun querySubscriptionDetails() {
        querySpecificProduct(SUBSCRIPTION_SKU, BillingClient.ProductType.SUBS) { success ->
            if (!success) {
                Log.w(TAG, "Real subscription product not found, trying test products...")
                querySpecificProduct(TEST_SUBSCRIPTION_SKU, BillingClient.ProductType.INAPP) { testSuccess ->
                    if (!testSuccess) {
                        Log.e(TAG, "Both real and test products failed")
                        showNoProductsAvailable()
                    }
                }
            }
        }
    }

    private fun querySpecificProduct(productId: String, productType: String, callback: (Boolean) -> Unit) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (productDetailsList.isNotEmpty()) {
                    productDetails = productDetailsList[0]
                    Log.d(TAG, "Product details retrieved successfully for: $productId")

                    if (productType == BillingClient.ProductType.SUBS) {
                        productDetails?.subscriptionOfferDetails?.let { offers ->
                            Log.d(TAG, "Available subscription offers: ${offers.size}")
                            offers.forEachIndexed { index, offer ->
                                Log.d(TAG, "Offer $index: basePlanId=${offer.basePlanId}, offerToken=${offer.offerToken}")
                            }
                        } ?: Log.w(TAG, "No subscription offers found")
                    }
                    callback(true)
                } else {
                    Log.e(TAG, "No product details found for: $productId")
                    callback(false)
                }
            } else {
                Log.e(TAG, "Failed to query product details for $productId: ${billingResult.debugMessage}")
                callback(false)
            }
        }
    }

    private fun showNoProductsAvailable() {
        runOnUiThread {
            Toast.makeText(this, "No subscription products available. Check your setup in Play Console.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        binding.btnSkipForNow.setOnClickListener {
            navigateToMain()
        }
        binding.btnSubscribe.setOnClickListener {
            launchSubscriptionFlow()
        }
    }

    private fun launchSubscriptionFlow() {
        if (!billingClient.isReady) {
            Log.e(TAG, "Billing client is not ready")
            Toast.makeText(this, "Billing service not ready. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (productDetails != null) {
            val productDetailsParamsList = if (productDetails!!.productType == BillingClient.ProductType.SUBS) {
                val subscriptionOfferDetails = productDetails!!.subscriptionOfferDetails

                if (subscriptionOfferDetails.isNullOrEmpty()) {
                    Log.e(TAG, "No subscription offers available")
                    Toast.makeText(this, "No subscription offers available", Toast.LENGTH_SHORT).show()
                    return
                }

                val selectedOffer = subscriptionOfferDetails[0]
                Log.d(TAG, "Using subscription offer: basePlanId=${selectedOffer.basePlanId}, offerToken=${selectedOffer.offerToken}")

                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails!!)
                        .setOfferToken(selectedOffer.offerToken)
                        .build()
                )
            } else {
                Log.d(TAG, "Using in-app product: ${productDetails!!.productId}")
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails!!)
                        .build()
                )
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val billingResult = billingClient.launchBillingFlow(this, billingFlowParams)

            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Billing flow launched successfully")
            } else {
                Log.e(TAG, "Failed to launch billing flow: ${billingResult.debugMessage}")
                Toast.makeText(this, "Failed to start subscription process: ${billingResult.debugMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e(TAG, "No product details available")
            Toast.makeText(this, "Subscription not available. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        Log.d(TAG, "onPurchasesUpdated called - Response Code: ${billingResult.responseCode}")

        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                Log.d(TAG, "Purchase successful")
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User canceled the purchase")
                Toast.makeText(this, "Purchase canceled", Toast.LENGTH_SHORT).show()
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                Toast.makeText(this, "You already have an active subscription", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
            else -> {
                Log.e(TAG, "Purchase failed: ${billingResult.debugMessage}")
                Toast.makeText(this, "Purchase failed: ${getResponseCodeMessage(billingResult.responseCode)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun getResponseCodeMessage(responseCode: Int): String {
        return when (responseCode) {
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT -> "Service timeout"
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Feature not supported"
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> "Service disconnected"
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Billing unavailable"
            BillingClient.BillingResponseCode.NETWORK_ERROR -> "Network error"
            else -> "Unknown error (Code: $responseCode)"
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged successfully")
                        onSubscriptionSuccess()
                    } else {
                        Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
                    }
                }
            } else {
                onSubscriptionSuccess()
            }
        }
    }

    private fun onSubscriptionSuccess() {
        Toast.makeText(this, "Subscription successful! Welcome to Premium!", Toast.LENGTH_LONG).show()

        val sharedPref = getSharedPreferences("subscription_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("is_premium_user", true)
            putLong("subscription_time", System.currentTimeMillis())
            apply()
        }

        navigateToMain()
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}
