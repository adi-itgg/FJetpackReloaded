package me.phantomx.fjetpackreloaded.test

import be.seeseemelk.mockbukkit.MockBukkit
import be.seeseemelk.mockbukkit.ServerMock
import me.phantomx.fjetpackreloaded.FJetpackReloaded
import me.phantomx.fjetpackreloaded.data.Messages
import org.junit.After
import org.junit.Before
import org.junit.Test


class Test {

    private lateinit var server: ServerMock
    private lateinit var plugin: FJetpackReloaded

    //@Before
    fun setUp() {
        server = MockBukkit.mock()
        plugin = MockBukkit.load(FJetpackReloaded::class.java) as FJetpackReloaded
    }

    //@After
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun test() {
        print("isOk")
    }

    @Test
    fun reflectTest() {
        val r = Messages().safeStringsClassYaml()
        println(r)
    }

}