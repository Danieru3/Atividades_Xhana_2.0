package org.example.tdd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CalculadoraDeDescontosTest {

    private CalculadoraDeDescontos calc;

    @BeforeEach
    void setUp() {
        // Antes de cada teste, temos uma nova instância
        calc = new CalculadoraDeDescontos();
    }

    @Test
    @DisplayName("Regra 1: Compras abaixo de R$100 não devem ter desconto")
    void deveRetornarValorIntegralParaCompraAbaixoDeCem() {
        assertEquals(99.0, calc.calcular(99.0));
        assertEquals(50.0, calc.calcular(50.0));
        assertEquals(0.0, calc.calcular(0.0));
    }

    @Test
    @DisplayName("Regra 2: Compras entre R$100 e R$500 devem ter 5% de desconto")
    void deveAplicar5PorcentoDescontoEntreCemEQuinhentos() {
        // Limite inferior (exato 100)
        assertEquals(95.0, calc.calcular(100.0), "Valor exato de R$100.00 deve ter 5%");
        
        // Valor no meio
        assertEquals(237.5, calc.calcular(250.0), "Valor de R$250.00 deve ter 5%");
        
        // Limite superior (exato 500)
        assertEquals(475.0, calc.calcular(500.0), "Valor exato de R$500.00 deve ter 5%");
    }

    @Test
    @DisplayName("Regra 3: Compras acima de R$500 devem ter 10% de desconto")
    void deveAplicar10PorcentoDescontoAcimaDeQuinhentos() {
        // Limite (um centavo acima)
        assertEquals(450.009, calc.calcular(500.01), "R$500.01 deve ter 10%");
        
        // Valor maior
        assertEquals(900.0, calc.calcular(1000.0), "R$1000.00 deve ter 10%");
    }

    @Test
    @DisplayName("Regra 4: Valor negativo deve lançar IllegalArgumentException")
    void deveLancarExcecaoParaValorNegativo() {
        // Dica do TDD01.MD
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            calc.calcular(-50.0);
        });
        
        // Verificação extra
        assertEquals("Valor da compra não pode ser negativo.", ex.getMessage());
    }
}