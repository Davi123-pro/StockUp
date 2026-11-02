package com.example.stockup.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY expirationDate ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    /**
     * Versão observável da consulta acima. A tela de detalhes precisa dela para
     * refletir edições na hora — com a leitura única, mudar a validade só aparecia
     * depois de sair do produto e entrar de novo.
     */
    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductFlow(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): Product?

    /**
     * Produtos perto de vencer, dentro de uma JANELA.
     *
     * O piso ([floorTimestamp]) existe para parar a fadiga de alerta: sem ele, um
     * produto vencido há seis meses casa todo dia, para sempre, e o aviso de hoje —
     * que é o que importa — se perde no meio de um monte de avisos velhos.
     */
    @Query(
        "SELECT * FROM products WHERE expirationDate > 0 " +
            "AND expirationDate <= :thresholdTimestamp AND expirationDate >= :floorTimestamp"
    )
    suspend fun getProductsExpiringSoon(thresholdTimestamp: Long, floorTimestamp: Long): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Long)

    /** Usado só pela restauração de cópia de segurança, que substitui tudo. */
    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    /** Apaga o produto de exemplo do tour (ver MARCADOR_EXEMPLO_TOUR). */
    @Query("DELETE FROM products WHERE notes = :marcador")
    suspend fun deleteByMarcador(marcador: String)
}
