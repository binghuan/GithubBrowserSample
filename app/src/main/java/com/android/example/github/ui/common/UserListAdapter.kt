package com.android.example.github.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import com.android.example.github.AppExecutors
import com.android.example.github.R
import com.android.example.github.databinding.UserItemBinding
import com.android.example.github.vo.User

/**
 * A RecyclerView adapter for [User] class.
 */
class UserListAdapter(
    private val dataBindingComponent: DataBindingComponent,
    appExecutors: AppExecutors,
    private val showFullName: Boolean,
    private val itemClickCallback: ((User) -> Unit)?
) : DataBoundListAdapter<User, UserItemBinding>(
    appExecutors = appExecutors,
    diffCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.login == newItem.login
                    && oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.avatarUrl == newItem.avatarUrl
                    && oldItem.htmlUrl == newItem.htmlUrl
        }
    }
) {

    override fun createBinding(parent: ViewGroup): UserItemBinding {
        val binding = DataBindingUtil.inflate<UserItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.user_item,
            parent,
            false,
            dataBindingComponent
        )
        binding.showFullName = showFullName
        binding.root.setOnClickListener {
            binding.user?.let {
                itemClickCallback?.invoke(it)
            }
        }
        return binding
    }

    override fun bind(binding: UserItemBinding, item: User) {
        binding.user = item
    }
}
