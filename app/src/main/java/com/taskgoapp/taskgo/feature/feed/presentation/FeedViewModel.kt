package com.taskgoapp.taskgo.feature.feed.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.PostLocation
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.data.repository.FeedMediaRepository
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.domain.usecase.*
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentRadius: Double = 50.0, // Padrão 50km
    val userLocation: Pair<Double, Double>? = null, // latitude, longitude
    val userCity: String? = null,
    val userState: String? = null,
    val currentUserAvatarUrl: String? = null,
    val currentUserName: String = "",
    val canPost: Boolean = false, // Apenas prestadores e vendedores podem postar
    val selectedPost: Post? = null // Post selecionado para detalhes
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val likePostUseCase: LikePostUseCase,
    private val unlikePostUseCase: UnlikePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val feedMediaRepository: FeedMediaRepository,
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository,
    private val feedRepository: com.taskgoapp.taskgo.domain.repository.FeedRepository
) : ViewModel() {
    
    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()
    
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    
    private val _accountType = MutableStateFlow<AccountType?>(null)
    
    val currentUserId: String?
        get() = authRepository.getCurrentUser()?.uid
    
    init {
        loadCurrentUserProfile()
        loadUserLocation()
    }
    
    private fun startObservingFeed() {
        viewModelScope.launch {
            userRepository.observeCurrentUser()
                .distinctUntilChanged { old, new -> 
                    old?.city == new?.city && old?.state == new?.state 
                }
                .collect { user ->
                    val userCity = user?.city?.takeIf { it.isNotBlank() }
                    val userState = user?.state?.takeIf { it.isNotBlank() }

                    if (userCity != null && userState != null) {
                        _uiState.value = _uiState.value.copy(
                            userCity = userCity,
                            userState = userState
                        )
                        loadFeedOnce()
                    }
                }
        }
    }
    
    private fun loadFeedOnce() {
        val userCity = _uiState.value.userCity
        val userState = _uiState.value.userState

        if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            android.util.Log.d("FeedViewModel", "🔵 loadFeedOnce: Iniciando carregamento do feed")
            android.util.Log.d("FeedViewModel", "   userCity=$userCity, userState=$userState, accountType=${_accountType.value}")
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                android.util.Log.d("FeedViewModel", "🟢 Chamando getFeedPostsUseCase...")
                val flow = getFeedPostsUseCase(
                    0.0,
                    0.0,
                    _uiState.value.currentRadius
                )
                android.util.Log.d("FeedViewModel", "   Flow obtido, iniciando coleta com take(1)...")
                
                flow.take(1).collect { allPosts ->
                    android.util.Log.d("FeedViewModel", "🟡 COLECT: Recebidos ${allPosts.size} posts do UseCase")
                    
                    // REGRA DE NEGÓCIO:
                    // - CLIENTE: vê apenas posts de parceiros (não próprios, pois cliente não posta)
                    // - PARCEIRO: vê todos os posts (próprios + de outros parceiros)
                    val filteredPosts = when (_accountType.value) {
                        AccountType.CLIENTE -> {
                            android.util.Log.d("FeedViewModel", "   Filtro CLIENTE: mantendo todos os posts (filtro no repositório)")
                            allPosts
                        }
                        AccountType.PARCEIRO -> {
                            android.util.Log.d("FeedViewModel", "   Filtro PARCEIRO: mantendo todos os posts")
                            allPosts
                        }
                        else -> {
                            android.util.Log.w("FeedViewModel", "   Filtro DESCONHECIDO: accountType=${_accountType.value}, mantendo todos")
                            allPosts
                        }
                    }
                    
                    android.util.Log.d("FeedViewModel", "✅ Atualizando UI: ${filteredPosts.size} posts após filtro")
                    _uiState.value = _uiState.value.copy(
                        posts = filteredPosts,
                        isLoading = false
                    )
                    android.util.Log.d("FeedViewModel", "✅ UI atualizada com sucesso")
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "🔴 EXCEPTION em loadFeedOnce", e)
                android.util.Log.e("FeedViewModel", "   Tipo: ${e.javaClass.simpleName}, Mensagem: ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar feed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Carrega dados do perfil do usuário atual para exibir no feed
     */
    private fun loadCurrentUserProfile() {
        viewModelScope.launch {
            try {
                userRepository.observeCurrentUser().collect { user ->
                    user?.let {
                        // Apenas parceiros podem postar (unificação de prestadores e vendedores)
                        val canPost = it.accountType == AccountType.PARCEIRO
                        _accountType.value = it.accountType
                        _uiState.value = _uiState.value.copy(
                            currentUserAvatarUrl = it.avatarUri,
                            currentUserName = it.name,
                            canPost = canPost
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao carregar perfil do usuário: ${e.message}", e)
            }
        }
    }
    
    fun loadUserLocation() {
        startObservingFeed()
    }
    
    /**
     * Atualiza o feed (pull to refresh)
     */
    fun refreshFeed() {
        loadFeedOnce()
    }
    
    /**
     * Cria um novo post
     */
    fun createPost(text: String, mediaUris: List<Uri>) {
        val location = _uiState.value.userLocation
        val userId = currentUserId
        
        if (userId == null) {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado")
            return
        }
        
        // Verificar se o usuário pode postar (apenas prestadores e vendedores)
        if (!_uiState.value.canPost) {
            _uiState.value = _uiState.value.copy(error = "Apenas prestadores e vendedores podem criar posts")
            return
        }
        
        if (location == null) {
            _uiState.value = _uiState.value.copy(error = "Localização não disponível para criar post")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Determinar tipos de mídia (assumir que são imagens por padrão)
                // Em uma implementação completa, detectar tipo de arquivo
                val mediaTypes = mediaUris.map { uri ->
                    val path = uri.toString().lowercase()
                    when {
                        path.contains("video") || path.contains(".mp4") || path.contains(".mov") -> "video"
                        else -> "image"
                    }
                }
                
                // Fazer upload das mídias primeiro
                val mediaUrls = if (mediaUris.isNotEmpty()) {
                    val uploadResults = feedMediaRepository.uploadPostMediaBatch(
                        mediaUris,
                        userId,
                        mediaTypes
                    )
                    
                    when (uploadResults) {
                        is Result.Success -> uploadResults.data
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Erro ao fazer upload das mídias: ${uploadResults.exception.message}"
                            )
                            return@launch
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Erro desconhecido ao fazer upload das mídias"
                            )
                            return@launch
                        }
                    }
                } else {
                    emptyList()
                }
                
                // CRÍTICO: Obter city e state do perfil do usuário (fonte de verdade)
                val currentUser = userRepository.observeCurrentUser().first()
                val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
                val userState = currentUser?.state?.takeIf { it.isNotBlank() }
                
                if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Localização não disponível. Aguarde a localização ser detectada e tente novamente."
                    )
                    android.util.Log.w("FeedViewModel", "⚠️ Tentativa de criar post sem localização válida: city=$userCity, state=$userState")
                    return@launch
                }
                
                // Criar localização do post
                val postLocation = PostLocation(
                    city = userCity,
                    state = userState,
                    latitude = location.first,
                    longitude = location.second
                )
                
                // Criar post no Firestore
                val result = try {
                    createPostUseCase(text, mediaUrls, mediaTypes, postLocation)
                } catch (e: Exception) {
                    android.util.Log.e("FeedViewModel", "Erro ao chamar createPostUseCase: ${e.message}", e)
                    com.taskgoapp.taskgo.core.model.Result.Error(e)
                }
                
                when (result) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = null
                        )
                        // Feed será atualizado automaticamente pelo listener
                        android.util.Log.d("FeedViewModel", "Post criado com sucesso")
                    }
                    is Result.Error -> {
                        android.util.Log.e("FeedViewModel", "Erro ao criar post: ${result.exception.message}", result.exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao criar post: ${result.exception.message ?: "Erro desconhecido"}"
                        )
                    }
                    else -> {
                        android.util.Log.w("FeedViewModel", "Resultado desconhecido ao criar post: $result")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro desconhecido ao criar post"
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao criar post: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao criar post: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Adiciona like em um post
     */
    fun likePost(postId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            val result = likePostUseCase(postId, userId)
            if (result is Result.Error) {
                android.util.Log.e("FeedViewModel", "Erro ao curtir post: ${result.exception.message}", result.exception)
            }
        }
    }
    
    /**
     * Remove like de um post
     */
    fun unlikePost(postId: String) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            val result = unlikePostUseCase(postId, userId)
            if (result is Result.Error) {
                android.util.Log.e("FeedViewModel", "Erro ao descurtir post: ${result.exception.message}", result.exception)
            }
        }
    }
    
    /**
     * Deleta um post
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = deletePostUseCase(postId)
            if (result is Result.Error) {
                android.util.Log.e("FeedViewModel", "Erro ao deletar post: ${result.exception.message}", result.exception)
                _uiState.value = _uiState.value.copy(error = "Erro ao deletar post: ${result.exception.message}")
            }
        }
    }
    
    /**
     * Atualiza o raio de busca
     */
    fun updateRadius(newRadius: Double) {
        _uiState.value = _uiState.value.copy(currentRadius = newRadius)
        loadFeedOnce()
    }
    
    /**
     * Observa comentários de um post
     */
    fun observePostComments(postId: String): Flow<List<com.taskgoapp.taskgo.feature.feed.presentation.components.CommentItem>> {
        return feedRepository.observePostComments(postId)
    }
    
    /**
     * Cria um comentário em um post
     */
    fun createComment(postId: String, text: String) {
        viewModelScope.launch {
            val result = feedRepository.createComment(postId, text)
            when (result) {
                is Result.Success -> {
                    android.util.Log.d("FeedViewModel", "Comentário criado com sucesso: ${result.data}")
                }
                is Result.Error -> {
                    android.util.Log.e("FeedViewModel", "Erro ao criar comentário: ${result.exception.message}", result.exception)
                    _uiState.value = _uiState.value.copy(error = "Erro ao criar comentário: ${result.exception.message}")
                }
                is Result.Loading -> {
                    // Loading state - não precisa fazer nada
                }
            }
        }
    }
    
    /**
     * Variante suspensa para criação de comentário com retorno imediato de Result
     * Útil em cenários de UI que precisam sinalizar erro sem depender do listener
     */
    suspend fun createCommentAwait(postId: String, text: String): Result<String> {
        return try {
            feedRepository.createComment(postId, text)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    /**
     * Deleta um comentário
     */
    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            val result = feedRepository.deleteComment(postId, commentId)
            when (result) {
                is Result.Success -> {
                    android.util.Log.d("FeedViewModel", "Comentário deletado com sucesso")
                }
                is Result.Error -> {
                    android.util.Log.e("FeedViewModel", "Erro ao deletar comentário: ${result.exception.message}", result.exception)
                    _uiState.value = _uiState.value.copy(error = "Erro ao deletar comentário: ${result.exception.message}")
                }
                is Result.Loading -> {
                    // Loading state - não precisa fazer nada
                }
            }
        }
    }
    
    /**
     * Limpa o erro
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Carrega um post específico por ID
     */
    fun loadPostById(postId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedPost = null)
            try {
                val post = feedRepository.getPostById(postId)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedPost = post,
                    error = if (post == null) "Post não encontrado" else null
                )
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao carregar post: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar post: ${e.message}",
                    selectedPost = null
                )
            }
        }
    }
    
    /**
     * Define interesse em um post (Tenho interesse / Não tenho interesse)
     */
    fun setPostInterest(postId: String, isInterested: Boolean) {
        viewModelScope.launch {
            try {
                when (val result = feedRepository.setPostInterest(postId, isInterested)) {
                    is Result.Success -> {
                        android.util.Log.d("FeedViewModel", "Interesse definido: $isInterested para post $postId")
                        // Recarregar posts para atualizar o feed
                        refreshFeed()
                    }
                    is Result.Error -> {
                        android.util.Log.e("FeedViewModel", "Erro ao definir interesse: ${result.exception.message}")
                        _uiState.value = _uiState.value.copy(error = "Erro ao definir interesse: ${result.exception.message}")
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao definir interesse: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = "Erro ao definir interesse: ${e.message}")
            }
        }
    }
    
    /**
     * Remove interesse de um post
     */
    fun removePostInterest(postId: String) {
        viewModelScope.launch {
            try {
                when (val result = feedRepository.removePostInterest(postId)) {
                    is Result.Success -> {
                        android.util.Log.d("FeedViewModel", "Interesse removido do post $postId")
                        refreshFeed()
                    }
                    is Result.Error -> {
                        android.util.Log.e("FeedViewModel", "Erro ao remover interesse: ${result.exception.message}")
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao remover interesse: ${e.message}", e)
            }
        }
    }
    
    /**
     * Avalia um post (1-5 estrelas)
     */
    fun ratePost(postId: String, rating: Int, comment: String? = null) {
        viewModelScope.launch {
            try {
                when (val result = feedRepository.ratePost(postId, rating, comment)) {
                    is Result.Success -> {
                        android.util.Log.d("FeedViewModel", "Post $postId avaliado com $rating estrelas")
                        // Recarregar post para atualizar avaliação
                        loadPostById(postId)
                    }
                    is Result.Error -> {
                        android.util.Log.e("FeedViewModel", "Erro ao avaliar post: ${result.exception.message}")
                        _uiState.value = _uiState.value.copy(error = "Erro ao avaliar post: ${result.exception.message}")
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao avaliar post: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = "Erro ao avaliar post: ${e.message}")
            }
        }
    }
    
    /**
     * Obtém avaliação do usuário atual para um post
     */
    suspend fun getUserPostRating(postId: String): com.taskgoapp.taskgo.core.model.PostRating? {
        return feedRepository.getUserPostRating(postId)
    }
    
    /**
     * Bloqueia um usuário
     */
    fun blockUser(userId: String) {
        viewModelScope.launch {
            try {
                when (val result = feedRepository.blockUser(userId)) {
                    is Result.Success -> {
                        android.util.Log.d("FeedViewModel", "Usuário $userId bloqueado")
                        // Recarregar feed para remover posts do usuário bloqueado
                        refreshFeed()
                    }
                    is Result.Error -> {
                        android.util.Log.e("FeedViewModel", "Erro ao bloquear usuário: ${result.exception.message}")
                        _uiState.value = _uiState.value.copy(error = "Erro ao bloquear usuário: ${result.exception.message}")
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao bloquear usuário: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = "Erro ao bloquear usuário: ${e.message}")
            }
        }
    }
    
    /**
     * Desbloqueia um usuário
     */
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            try {
                when (val result = feedRepository.unblockUser(userId)) {
                    is Result.Success -> {
                        android.util.Log.d("FeedViewModel", "Usuário $userId desbloqueado")
                        refreshFeed()
                    }
                    is Result.Error -> {
                        android.util.Log.e("FeedViewModel", "Erro ao desbloquear usuário: ${result.exception.message}")
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("FeedViewModel", "Erro ao desbloquear usuário: ${e.message}", e)
            }
        }
    }
    
    /**
     * Verifica se um usuário está bloqueado
     */
    suspend fun isUserBlocked(userId: String): Boolean {
        return feedRepository.isUserBlocked(userId)
    }
}
