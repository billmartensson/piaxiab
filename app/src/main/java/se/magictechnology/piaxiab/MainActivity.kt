package se.magictechnology.piaxiab

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.android.billingclient.api.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    lateinit var billingClient: BillingClient

    var products : List<SkuDetails>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val purchasesUpdatedListener =
            PurchasesUpdatedListener { billingResult, purchases ->
                // To be implemented in a later section.

                if(billingResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED)
                {
                    Log.i("PIAXDEBUG", "ÄGER REDAN")
                }
                if(billingResult.responseCode == BillingClient.BillingResponseCode.OK)
                {
                    purchases?.let {  plist ->
                        plist.firstOrNull()?.let {  thepurchase ->
                            if (!thepurchase.isAcknowledged) {
                                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(thepurchase.purchaseToken)

                                billingClient.acknowledgePurchase(acknowledgePurchaseParams.build()) {
                                    Log.i("PIAXDEBUG", "OK BUY")

                                    //billingClient.endConnection()
                                }
                            }
                        }
                    }
                }



            }

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.i("PIAXDEBUG", "SETUP OK")
                    /*
                    runBlocking {
                        launch {
                            getproducts()
                        }
                    }
                     */
                    getproductsasync()
                } else {
                    Log.i("PIAXDEBUG", "SETUP NOT OK")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })

        findViewById<Button>(R.id.buyPremiumButton).setOnClickListener {
            for(prod in products!!)
            {
                if(prod.sku == "premium")
                {
                    buyProduct(prod)
                }
            }
        }

        findViewById<Button>(R.id.buyCreditButton).setOnClickListener {
            for(prod in products!!)
            {
                if(prod.sku == "credit")
                {
                    buyProduct(prod)
                }
            }
        }


    }


    suspend fun getproducts() {
        val skuList = ArrayList<String>()
        skuList.add("premium")
        skuList.add("credit")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        // leverage querySkuDetails Kotlin extension function
        val skuDetailsResult = withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }

        // Process the result.

        //buyProduct(skuDetailsResult.skuDetailsList!!.first())

        for(prod in skuDetailsResult.skuDetailsList!!)
        {
            Log.i("PIAXDEBUG", prod.title)
        }

        products = skuDetailsResult.skuDetailsList!!
        doProductUI()

    }

    fun getproductsasync()
    {
        val skuList = ArrayList<String>()
        skuList.add("premium")
        skuList.add("credit")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient!!.querySkuDetailsAsync(params.build()) { responseCode, skuDetailsList ->
            if (responseCode.responseCode == BillingClient.BillingResponseCode.OK) {

                Log.i("PIAXDEBUG", "GOT PRODUCTS " + skuDetailsList!!.size.toString())

                for(prod in skuDetailsList)
                {
                    Log.i("PIAXDEBUG", prod.title)
                }

                products = skuDetailsList
                doProductUI()

            } else {
                // TODO: Error no response
                Log.i("PIAXDEBUG", "ERROR PRODUCTS ")
            }
        }
    }

    fun buyProduct(theproduct : SkuDetails)
    {
        val flowParams = BillingFlowParams.newBuilder()
            .setSkuDetails(theproduct)
            .build()
        val responseCode = billingClient.launchBillingFlow(this, flowParams).responseCode
    }

    fun doProductUI()
    {
        for(prod in products!!)
        {
            if(prod.sku == "premium")
            {
                findViewById<TextView>(R.id.premiumTitleTextview).text = prod.title
                findViewById<TextView>(R.id.premiumDescriptionTextview).text = prod.description
            }
            if(prod.sku == "credit")
            {
                findViewById<TextView>(R.id.creditTitleTextview).text = prod.title
                findViewById<TextView>(R.id.creditDescptionTextview).text = prod.description
            }
        }
    }

    fun checkHistory()
    {
        Log.i("PIAXDEBUG", "CHECK HISTORY")
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP, { billingResultQuery, purchasesList ->
            if (billingResultQuery.responseCode == BillingClient.BillingResponseCode.OK) {

                val purchasesResult: Purchase.PurchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)

                for (purchase in purchasesResult.purchasesList!!) {
                    //val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken)

                    Log.i("PIAXDEBUG", "HAVE BOUGHT "+purchase.skus.first())

                }

            }
        })
    }
}