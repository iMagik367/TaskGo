# TaskGo Data Layer

Camada de dados completa para o aplicativo TaskGo usando Room Database, Repository Pattern e Kotlin Flow.

## 🏗️ Arquitetura

### Estrutura de Pacotes
```
core/data/
├── local/                    # Camada de persistência local
│   ├── entity/              # Entidades do Room
│   ├── dao/                 # Data Access Objects
│   ├── converter/           # TypeConverters para Room
│   └── TaskDatabase.kt      # Database principal
├── mapper/                  # Mapeadores Entity ↔ Model
├── repository/              # Repositórios
└── DataLayer.kt             # Arquivo de índice
```

## 📊 Modelos

### Task
```kotlin
@Serializable
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val tags: List<String> = emptyList()
)
```

### Priority
```kotlin
enum class Priority { LOW, MEDIUM, HIGH }
```

## 🗄️ Room Database

### TaskEntity
```kotlin
@Entity(tableName = "tasks")
@TypeConverters(InstantConverter::class, StringListConverter::class)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)
```

### TypeConverters
- **InstantConverter**: Converte `Instant` ↔ `Long` para armazenamento
- **StringListConverter**: Converte `List<String>` ↔ `String` usando Gson

## 🔍 Data Access Object (DAO)

### Operações Principais
```kotlin
@Dao
interface TaskDao {
    // Queries com Flow
    fun getAllTasks(): Flow<List<TaskEntity>>
    fun getPendingTasks(): Flow<List<TaskEntity>>
    fun getCompletedTasks(): Flow<List<TaskEntity>>
    
    // Queries específicas
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    fun getTasksByPriority(priority: Priority): Flow<List<TaskEntity>>
    fun getTasksByDateRange(startDate: Instant, endDate: Instant): Flow<List<TaskEntity>>
    
    // Operações CRUD
    suspend fun getTaskById(taskId: Long): TaskEntity?
    suspend fun insertTask(task: TaskEntity): Long
    suspend fun updateTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)
    
    // Operações de conveniência
    suspend fun updateTaskDone(taskId: Long, done: Boolean, updatedAt: Instant)
    suspend fun deleteCompletedTasks()
    
    // Contadores
    fun getPendingTasksCount(): Flow<Int>
    fun getCompletedTasksCount(): Flow<Int>
}
```

## 🔄 Mapeadores

### TaskMapper
```kotlin
object TaskMapper {
    fun TaskEntity.toTask(): Task
    fun Task.toTaskEntity(): TaskEntity
    fun List<TaskEntity>.toTaskList(): List<Task>
    fun List<Task>.toTaskEntityList(): List<TaskEntity>
}
```

**Características:**
- Mapeamento bidirecional entre Entity e Model
- Preservação de timestamps automática
- Conversão de listas em lote

## 📚 Repository

### TaskRepository
```kotlin
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
)
```

#### Streams com Flow
- `getAllTasks()`: Todas as tarefas ordenadas por criação
- `getPendingTasks()`: Tarefas pendentes ordenadas por prazo e prioridade
- `getCompletedTasks()`: Tarefas concluídas ordenadas por atualização
- `searchTasks(query)`: Busca por título ou descrição
- `getTasksByPriority(priority)`: Filtro por prioridade
- `getTasksByDateRange(start, end)`: Filtro por intervalo de datas

#### Operações CRUD
- `getTaskById(id)`: Busca tarefa específica
- `insertTask(task)`: Insere nova tarefa
- `updateTask(task)`: Atualiza tarefa existente
- `toggleTaskDone(id, done)`: Altera status de conclusão
- `deleteTask(task)`: Remove tarefa
- `deleteTaskById(id)`: Remove por ID

#### Operações de Conveniência
- `createTask(title, description, dueAt, priority, tags)`: Criação simplificada
- `markTaskAsDone(id)`: Marca como concluída
- `markTaskAsPending(id)`: Marca como pendente
- `updateTaskPriority(id, priority)`: Atualiza prioridade
- `updateTaskDueDate(id, dueAt)`: Atualiza prazo
- `addTagToTask(id, tag)`: Adiciona tag
- `removeTagFromTask(id, tag)`: Remove tag

## 🧪 Testes Unitários

### Cobertura de Testes
- ✅ **TaskMapper**: Mapeamento bidirecional e listas
- ✅ **TaskRepository**: Operações CRUD e streams
- ✅ **TypeConverters**: Conversão de tipos

### Executar Testes
```bash
# Todos os testes
./gradlew test

# Testes específicos
./gradlew test --tests "*TaskMapperTest*"
./gradlew test --tests "*TaskRepositoryTest*"
./gradlew test --tests "*TypeConvertersTest*"
```

## 🚀 Como Usar

### 1. Configurar Dependências
```kotlin
// build.gradle.kts
dependencies {
    implementation "androidx.room:room-runtime:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"
    
    implementation "com.google.code.gson:gson:2.10.1"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0"
}
```

### 2. Inicializar Database
```kotlin
class TaskGoApplication : Application() {
    val database: TaskDatabase by lazy { TaskDatabase.getDatabase(this) }
}
```

### 3. Usar Repository
```kotlin
@Inject
lateinit var taskRepository: TaskRepository

// Observar todas as tarefas
val allTasks = taskRepository.getAllTasks()

// Criar nova tarefa
val taskId = taskRepository.createTask(
    title = "Nova Tarefa",
    description = "Descrição da tarefa",
    priority = Priority.HIGH
)

// Marcar como concluída
taskRepository.markTaskAsDone(taskId)

// Buscar tarefas pendentes
val pendingTasks = taskRepository.getPendingTasks()
```

### 4. Observar Mudanças com Flow
```kotlin
@Composable
fun TaskList() {
    val tasks by taskRepository.getAllTasks()
        .collectAsState(initial = emptyList())
    
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task = task)
        }
    }
}
```

## 🔧 Configurações

### Database Builder
```kotlin
Room.databaseBuilder(
    context.applicationContext,
    TaskDatabase::class.java,
    "task_database"
)
.fallbackToDestructiveMigration()  // Para desenvolvimento
.build()
```

### TypeConverters
```kotlin
@TypeConverters(
    InstantConverter::class,
    StringListConverter::class
)
```

## 📱 Integração com UI

### ViewModel
```kotlin
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    val tasks = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun addTask(title: String) {
        viewModelScope.launch {
            taskRepository.createTask(title = title)
        }
    }
}
```

### Composable
```kotlin
@Composable
fun TaskScreen(viewModel: TaskViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsState()
    
    // UI implementation
}
```

## 🎯 Características

### ✅ Implementado
- **Room Database** com Entity, DAO e Database
- **Repository Pattern** com operações CRUD completas
- **Kotlin Flow** para streams reativos
- **TypeConverters** para Instant e List<String>
- **Mapeadores** bidirecionais Entity ↔ Model
- **Operações de conveniência** para casos comuns
- **Testes unitários** com Mockito e JUnit
- **Injeção de dependência** com Hilt

### 🔄 Fluxo de Dados
```
UI → ViewModel → Repository → DAO → Database
                ↓
            Flow<Task> ← Mapper ← Entity
```

### 🚨 Boas Práticas
1. **Sempre use o Repository** para acessar dados
2. **Observe mudanças com Flow** para UI reativa
3. **Use operações suspend** para operações assíncronas
4. **Mapeie Entity para Model** antes de usar na UI
5. **Teste todas as operações** com mocks apropriados

## 📚 Recursos Adicionais

- [Room Documentation](https://developer.android.com/training/data-storage/room)
- [Kotlin Flow](https://kotlinlang.org/docs/flow.html)
- [Repository Pattern](https://developer.android.com/jetpack/guide#recommended-app-arch)
- [Testing with Room](https://developer.android.com/training/data-storage/room/testing-db)
