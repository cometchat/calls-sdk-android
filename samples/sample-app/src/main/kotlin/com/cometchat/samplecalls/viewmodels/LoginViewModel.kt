package com.cometchat.samplecalls.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException
import com.cometchat.calls.model.CallUser
import com.cometchat.samplecalls.data.repository.Repository
import com.cometchat.samplecalls.data.repository.SampleUser

class LoginViewModel : ViewModel() {

    val loginStatus: MutableLiveData<Boolean> = MutableLiveData()
    val selectedUser: MutableLiveData<SampleUser?> = MutableLiveData()
    val users: MutableLiveData<List<SampleUser>> = MutableLiveData()
    private val _error = MutableLiveData<CometChatException>()

    fun onError(): LiveData<CometChatException> = _error

    fun checkUserIsNotLoggedIn() {
        loginStatus.value = CometChatCalls.getLoggedInUser() != null
    }

    fun fetchSampleUsers() {
        Repository.fetchSampleUsers(object : CometChatCalls.CallbackListener<List<SampleUser>>() {
            override fun onSuccess(sampleUsers: List<SampleUser>) {
                users.postValue(sampleUsers)
            }

            override fun onError(e: CometChatException) {
                users.postValue(emptyList())
            }
        })
    }

    fun selectUser(user: SampleUser?) {
        selectedUser.value = user
    }

    fun login(uid: String, apiKey: String) {
        Repository.loginUser(uid, apiKey, object : CometChatCalls.CallbackListener<CallUser>() {
            override fun onSuccess(user: CallUser) {
                loginStatus.postValue(true)
            }

            override fun onError(e: CometChatException) {
                loginStatus.postValue(false)
                _error.postValue(e)
            }
        })
    }
}
