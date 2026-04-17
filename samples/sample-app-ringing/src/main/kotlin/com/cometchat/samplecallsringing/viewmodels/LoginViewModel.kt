package com.cometchat.samplecallsringing.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cometchat.calls.core.CometChatCalls
import com.cometchat.calls.exceptions.CometChatException as CallsException
import com.cometchat.calls.model.CallUser
import com.cometchat.chat.core.CometChat
import com.cometchat.chat.exceptions.CometChatException as ChatException
import com.cometchat.chat.models.User
import com.cometchat.samplecallsringing.data.repository.Repository
import com.cometchat.samplecallsringing.data.repository.SampleUser

class LoginViewModel : ViewModel() {

    val loginStatus: MutableLiveData<Boolean> = MutableLiveData()
    val selectedUser: MutableLiveData<SampleUser?> = MutableLiveData()
    val users: MutableLiveData<List<SampleUser>> = MutableLiveData()
    private val _error = MutableLiveData<Exception>()

    fun onError(): LiveData<Exception> = _error

    fun checkUserIsNotLoggedIn(): Boolean {
        return CometChat.getLoggedInUser() == null
    }

    fun fetchSampleUsers() {
        Repository.fetchSampleUsers(object : CometChatCalls.CallbackListener<List<SampleUser>>() {
            override fun onSuccess(sampleUsers: List<SampleUser>) {
                users.postValue(sampleUsers)
            }

            override fun onError(e: CallsException) {
                users.postValue(emptyList())
            }
        })
    }

    fun selectUser(user: SampleUser?) {
        selectedUser.value = user
    }

    /**
     * Dual SDK login: Chat SDK first, then Calls SDK on success.
     */
    fun login(uid: String, apiKey: String) {
        Repository.loginChatUser(uid, apiKey, object : CometChat.CallbackListener<User>() {
            override fun onSuccess(user: User) {
                Repository.loginCallsUser(uid, apiKey, object : CometChatCalls.CallbackListener<CallUser>() {
                    override fun onSuccess(callUser: CallUser) {
                        loginStatus.postValue(true)
                    }

                    override fun onError(e: CallsException) {
                        loginStatus.postValue(false)
                        _error.postValue(e)
                    }
                })
            }

            override fun onError(e: ChatException) {
                loginStatus.postValue(false)
                _error.postValue(e)
            }
        })
    }
}
