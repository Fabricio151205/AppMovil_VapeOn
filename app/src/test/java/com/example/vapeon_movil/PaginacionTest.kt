package com.example.vapeon_movil

import com.example.vapeon_movil.services.*
import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PaginacionTest {
    private fun retrofit(responder: (Request) -> Pair<Int, String>): Retrofit = Retrofit.Builder()
        .baseUrl("https://firestore.test/documents/")
        .client(OkHttpClient.Builder().addInterceptor {
            val request = it.request()
            val (codigo, contenido) = responder(request)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(codigo).message("Respuesta")
                .body(ResponseBody.create(MediaType.parse("application/json"), contenido)).build()
        }.build()).addConverterFactory(GsonConverterFactory.create()).build()

    @Test fun productosDeDosPaginasSeReunenConElTokenReal() = runBlocking {
        val tokens = mutableListOf<String?>()
        val api = retrofit {
            val token = it.url().queryParameter("pageToken")
            tokens += token
            200 to if (token == null) """{"documents":[{"name":"productos/p1","fields":{}}],"nextPageToken":"siguiente"}"""
                else """{"documents":[{"name":"productos/p2","fields":{}}]}"""
        }.create(ProductoService::class.java)
        assertEquals(listOf("productos/p1", "productos/p2"), api.listarTodosProductos().documents!!.map { it.name })
        assertEquals(listOf(null, "siguiente"), tokens)
    }

    @Test fun usuariosDePaginasPosterioresNoQuedanFueraDelLogin() = runBlocking {
        val api = retrofit {
            200 to if (it.url().queryParameter("pageToken") == null)
                """{"documents":[],"nextPageToken":"pagina2"}"""
            else """{"documents":[{"name":"usuarios/u2","fields":{"correo":{"stringValue":"b@example.com"}}}]}"""
        }.create(UsuarioService::class.java)
        assertEquals("b@example.com", api.listarTodosUsuarios().documents!!.single().fields.correo?.stringValue)
    }

    @Test fun falloEnSegundaPaginaNoDevuelveListaParcial() = runBlocking {
        val api = retrofit {
            if (it.url().queryParameter("pageToken") == null) 200 to """{"documents":[],"nextPageToken":"pagina2"}"""
            else 403 to """{"error":{"status":"PERMISSION_DENIED"}}"""
        }.create(ProductoService::class.java)
        try { api.listarTodosProductos(); fail("Debe informar del error") }
        catch (e: retrofit2.HttpException) { assertEquals(403, e.code()) }
    }

    @Test fun tokenRepetidoSeDetieneConErrorControlado() = runBlocking {
        val api = retrofit { 200 to """{"documents":[],"nextPageToken":"repetido"}""" }
            .create(ProductoService::class.java)
        try { api.listarTodosProductos(); fail("No debe entrar en un bucle infinito") }
        catch (_: IllegalStateException) { }
    }
}
