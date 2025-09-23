package br.com.taskgo.taskgo.data.repository

import com.example.taskgoapp.data.local.dao.AddressDao
import com.example.taskgoapp.data.mapper.AddressMapper.toEntity
import com.example.taskgoapp.data.mapper.AddressMapper.toModel
import com.example.taskgoapp.domain.repository.AddressRepository
import com.example.taskgoapp.core.model.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressRepositoryImpl @Inject constructor(
    private val addressDao: AddressDao
) : AddressRepository {

    override fun observeAddresses(): Flow<List<Address>> {
        return addressDao.observeAll().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getAddress(id: String): Address? {
        return addressDao.getById(id)?.toModel()
    }

    override suspend fun upsertAddress(address: Address) {
        addressDao.upsert(address.toEntity())
    }

    override suspend fun deleteAddress(id: String) {
        val entity = addressDao.getById(id) ?: return
        addressDao.delete(entity)
    }
}