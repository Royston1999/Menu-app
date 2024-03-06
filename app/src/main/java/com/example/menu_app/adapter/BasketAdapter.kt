package com.example.menu_app.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.menu.R
import com.example.menu_app.database.basket.CartDAO
import com.example.menu_app.database.basket.CartItem
import com.example.menu_app.viewModel.mainViewModel
import kotlinx.coroutines.*

class BasketAdapter(private val cartDAO: CartDAO, private val mainVM: mainViewModel, private val lifecycleOwner: LifecycleOwner) : RecyclerView.Adapter<BasketAdapter.ViewHolder>(){

    private var cartItems: MutableList<CartItem> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cart_dishes_item, parent, false)
        return ViewHolder(view, mainVM)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cartItem = cartItems[position]
        holder.bind(cartItem)
    }

    override fun getItemCount(): Int {
        return cartItems.size
    }

    fun deleteItem(position: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            if (cartItems.isNotEmpty()){
                cartDAO.deleteCartItem(cartItems[position])
                cartItems.removeAt(position)
                withContext(Dispatchers.Main) {
                    notifyItemRemoved(position)
                }
            }
            Log.d("BasketAdapter", "cartItem deleted")
        }
    }

    fun setCartItems(cartItems: MutableList<CartItem>){
        this.cartItems = cartItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View, mainVM: mainViewModel) : RecyclerView.ViewHolder(view) {

        private val editQuantity = view.findViewById<View>(R.id.add_reduce_dish)
        private val priceTextView = view.findViewById<TextView>(R.id.basket_dish_price)
        private val basketDishContainer = view.findViewById<LinearLayout>(R.id.basket_dish_container)
        private val coroutineScope = CoroutineScope(Dispatchers.IO)

        init {
            val params = basketDishContainer.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.START_OF, priceTextView.id)

            mainVM.isEditModeEnabled.observe(lifecycleOwner) {
                if (it) {
                    editQuantity.visibility = View.VISIBLE
                    priceTextView.visibility = View.GONE
                    params.addRule(RelativeLayout.START_OF, editQuantity.id)

                } else {
                    editQuantity.visibility = View.GONE
                    priceTextView.visibility = View.VISIBLE
                    params.addRule(RelativeLayout.START_OF, priceTextView.id)
                }
                basketDishContainer.layoutParams = params
            }
        }

        fun bind(cartItem: CartItem){
            itemView.findViewById<TextView>(R.id.basket_dish_name).text = cartItem.name
            itemView.findViewById<TextView>(R.id.basket_dish_price).text = String.format("£%.2f", cartItem.price*cartItem.quantity)
            itemView.findViewById<TextView>(R.id.basket_dish_number_counter).text = cartItem.quantity.toString()
            itemView.findViewById<TextView>(R.id.basket_dish_counter).text = cartItem.quantity.toString()

            val notes = itemView.findViewById<TextView>(R.id.basket_dish_note)
            notes.text = cartItem.notes

            if(cartItem.notes.isNullOrEmpty()){
                notes.visibility = View.GONE
            } else {
                notes.visibility = View.VISIBLE
            }

            // Add onClickListeners for the quantity adjustment buttons
            itemView.findViewById<ImageView>(R.id.add_dish_button).setOnClickListener{
                coroutineScope.launch {
                    cartItem.quantity++
                    cartDAO.updateCartItem(cartItem)
                    mainVM.updateCart(cartItem)
                    withContext(Dispatchers.Main){
                        notifyItemChanged(adapterPosition)
                    }
                }
            }

            itemView.findViewById<ImageView>(R.id.reduce_dish_button).setOnClickListener{
                if (cartItem.quantity > 1){
                    coroutineScope.launch {
                        cartItem.quantity--
                        cartDAO.updateCartItem(cartItem)
                        mainVM.updateCart(cartItem)
                        withContext(Dispatchers.Main){
                            notifyItemChanged(adapterPosition)
                        }
                    }
                } else {
                    coroutineScope.launch {
                        //cartDAO.deleteCartItem(cartItem)
                        deleteItem(adapterPosition)
                        mainVM.updateCart(cartItem)
                        Log.d("BasketAdapter", "Deleted item: $cartItem")
                        Log.d("BasketAdapter", cartItems.toString())
                    }
                }
            }
        }
    }
}