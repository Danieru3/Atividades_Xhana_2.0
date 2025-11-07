package org.example.tdd;

/**
 * Implementação da classe CalculadoraDeDescontos, 
 * criada a partir dos testes (TDD).
 */
public class CalculadoraDeDescontos {

    /**
     * Calcula o valor final da compra com base nas regras de desconto.
     * @param valorCompra O valor original da compra.
     * @return O valor final com desconto aplicado.
     * @throws IllegalArgumentException Se o valorCompra for negativo.
     */
    public double calcular(double valorCompra) {
        
        // Regra 4: Validação de valor negativo
        if (valorCompra < 0) {
            throw new IllegalArgumentException("Valor da compra não pode ser negativo.");
        }

        // Regra 3: > 500 (10% de desconto)
        if (valorCompra > 500.0) {
            return valorCompra * 0.90;
        }

        // Regra 2: 100 <= valor <= 500 (5% de desconto)
        if (valorCompra >= 100.0) {
            return valorCompra * 0.95;
        }
        
        // Regra 1: < 100 (Sem desconto)
        return valorCompra;
    }
}