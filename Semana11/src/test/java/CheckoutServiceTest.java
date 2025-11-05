import org.example.checkout.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckoutServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2025, 11, 4);

    private CouponService couponSvc;
    private ShippingService shipSvc;
    private CheckoutService service;

    private final Item book = new Item("BOOK", 100.00, 1);
    private final Item eletronico = new Item("ELETRONICO", 50.00, 2); // 100.00
    private final List<Item> itensPadrao = List.of(book, eletronico);
    private final List<Item> apenasBook = List.of(book);

    @BeforeEach
    void setUp() {
        couponSvc = new CouponService();
        shipSvc = new ShippingService();
        service = new CheckoutService(couponSvc, shipSvc);
    }


    @Test
    @DisplayName("Teste Base: Deve calcular básico sem descontos e imposto apenas em não-book")
    public void deveCalcularBasicoSemDescontosEImpostoApenasNaoBook() {
        var res = service.checkout(
                itensPadrao,
                CustomerTier.BASIC,
                false,
                "SUL",
                3.0,
                null,
                TODAY,
                null
        );

        assertEquals(200.00, res.subtotal);
        assertEquals(0.00, res.discountValue);
        assertEquals(12.00, res.tax);
        assertEquals(35.00, res.shipping);
        assertEquals(247.00, res.total);
    }

    @Test
    @DisplayName("Validação Item: Deve lançar exceção para precoUnitario < 0")
    void deveLancarExcecaoParaPrecoNegativo() {
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            new Item("ELETRONICO", -10.0, 1);
        });
        assertEquals("precoUnitario < 0", ex.getMessage());
    }

    @Test
    @DisplayName("Validação Item: Deve lançar exceção para quantidade <= 0")
    void deveLancarExcecaoParaQuantidadeZero() {
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            new Item("BOOK", 10.0, 0);
        });
        assertEquals("quantidade <= 0", ex.getMessage());
    }

    @Test
    @DisplayName("Validação Shipping: Deve lançar exceção para weight < 0")
    void deveLancarExcecaoParaPesoNegativo() {
        var ex = assertThrows(IllegalArgumentException.class, () -> {
            shipSvc.calculate("SUL", -1.0, 100.0, false);
        });
        assertEquals("weight < 0", ex.getMessage());
    }
    
    @Test
    @DisplayName("Validação Checkout: Deve lançar NullPointerException para parâmetros nulos")
    void deveLancarExcecaoParaParametrosNulos() {
        assertThrows(NullPointerException.class, () -> {
            service.checkout(null, CustomerTier.BASIC, false, "SUL", 1.0, null, TODAY, null);
        }, "itens");
        
        assertThrows(NullPointerException.class, () -> {
            service.checkout(itensPadrao, null, false, "SUL", 1.0, null, TODAY, null);
        }, "tier");

        assertThrows(NullPointerException.class, () -> {
            service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 1.0, null, null, null);
        }, "today");
    }

    @Test
    @DisplayName("Desconto: Deve aplicar 5% para tier SILVER")
    void deveAplicarDescontoSilver() {
        var res = service.checkout(itensPadrao, CustomerTier.SILVER, false, "SUL", 3.0, null, TODAY, null);
        assertEquals(10.00, res.discountValue);
        assertEquals(236.40, res.total);
    }

    @Test
    @DisplayName("Desconto: Deve aplicar 10% para tier GOLD")
    void deveAplicarDescontoGold() {
        var res = service.checkout(itensPadrao, CustomerTier.GOLD, false, "SUL", 3.0, null, TODAY, null);
        assertEquals(20.00, res.discountValue);
        assertEquals(225.80, res.total);
    }

    @Test
    @DisplayName("Desconto: Deve aplicar 5% para primeira compra (subtotal >= 50)")
    void deveAplicarDescontoPrimeiraCompraAcima50() {
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, true, "SUL", 3.0, null, TODAY, null);
        assertEquals(10.00, res.discountValue);
        assertEquals(236.40, res.total);
    }

    @Test
    @DisplayName("Desconto: Não deve aplicar 5% para primeira compra (subtotal < 50)")
    void naoDeveAplicarDescontoPrimeiraCompraAbaixo50() {
        Item itemPequeno = new Item("ELETRONICO", 40.0, 1);
        var res = service.checkout(List.of(itemPequeno), CustomerTier.BASIC, true, "SUL", 1.0, null, TODAY, null);
        assertEquals(0.00, res.discountValue);
        assertEquals(64.80, res.total);
    }

    @Test
    @DisplayName("Desconto: Deve aplicar teto de 30% (GOLD + DESC20 + 1a Compra)")
    void deveAplicarTetoDeDesconto30Porcento() {
        var res = service.checkout(itensPadrao, CustomerTier.GOLD, true, "SUL", 3.0, "DESC20", TODAY, null);
        assertEquals(60.00, res.discountValue, "O desconto deve ser 60.00 (teto de 30%)");
        assertEquals(183.40, res.total);
    }

    @Test
    @DisplayName("Cupom: Deve aplicar DESC10")
    void deveAplicarCupomDesc10() {
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 3.0, "DESC10", TODAY, null);
        assertEquals(20.00, res.discountValue);
        assertEquals(225.80, res.total);
    }

    @Test
    @DisplayName("Cupom: Deve aplicar DESC20 (subtotal >= 100 e não expirado)")
    void deveAplicarCupomDesc20Valido() {
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 3.0, "DESC20", TODAY, null);
        assertEquals(40.00, res.discountValue);
        assertEquals(204.60, res.total);
    }

    @Test
    @DisplayName("Cupom: Não deve aplicar DESC20 (subtotal < 100)")
    void naoDeveAplicarCupomDesc20AbaixoMinimo() {
        Item itemMedio = new Item("ELETRONICO", 80.0, 1);
        var res = service.checkout(List.of(itemMedio), CustomerTier.BASIC, false, "SUL", 1.0, "DESC20", TODAY, null);
        assertEquals(0.00, res.discountValue);
        assertEquals(109.60, res.total);
    }
    
    @Test
    @DisplayName("Cupom: Não deve aplicar DESC20 (expirado)")
    void naoDeveAplicarCupomDesc20Expirado() {
        LocalDate dataExpiracao = TODAY.minusDays(1);
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 3.0, "DESC20", TODAY, dataExpiracao);
        assertEquals(0.00, res.discountValue);
        assertEquals(247.00, res.total);
    }

    @Test
    @DisplayName("Cupom: Não deve aplicar cupom nulo, vazio ou desconhecido")
    void naoDeveAplicarCupomInvalido() {
        var resVazio = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 3.0, "   ", TODAY, null);
        assertEquals(247.00, resVazio.total, "Cupom vazio deve ser ignorado");

        var resDesc = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 3.0, "CUPOM_FALSO", TODAY, null);
        assertEquals(247.00, resDesc.total, "Cupom desconhecido deve ser ignorado");
    }

    @Test
    @DisplayName("Frete: Deve ser grátis com cupom FRETEGRATIS (peso <= 5)")
    void deveAplicarFreteGratisComCupomAbaixoPeso() {
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 5.0, "FRETEGRATIS", TODAY, null);
        assertEquals(0.00, res.shipping);
        assertEquals(212.00, res.total);
    }

    @Test
    @DisplayName("Frete: Não deve ser grátis com cupom FRETEGRATIS (peso > 5)")
    void naoDeveAplicarFreteGratisComCupomAcimaPeso() {
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 5.1, "FRETEGRATIS", TODAY, null);
        assertEquals(50.00, res.shipping);
        assertEquals(262.00, res.total);
    }

    @Test
    @DisplayName("Frete: Deve ser grátis para subtotal com desconto >= 300")
    void deveAplicarFreteGratisSubtotalAcima300() {
        Item itemCaro = new Item("ELETRONICO", 400.0, 1);
        var res = service.checkout(List.of(itemCaro), CustomerTier.BASIC, false, "SUL", 10.0, null, TODAY, null);
        assertEquals(0.00, res.shipping);
        assertEquals(448.00, res.total);
    }

    @Test
    @DisplayName("Frete: Deve calcular frete para Região NORTE (todas as faixas)")
    void deveCalcularFreteRegiaoNorte() {
        var resPequeno = service.checkout(itensPadrao, CustomerTier.BASIC, false, "NORTE", 1.5, null, TODAY, null);
        assertEquals(30.00, resPequeno.shipping);
        assertEquals(242.00, resPequeno.total);
        var resMedio = service.checkout(itensPadrao, CustomerTier.BASIC, false, "NORTE", 4.0, null, TODAY, null);
        assertEquals(55.00, resMedio.shipping);
        assertEquals(267.00, resMedio.total);
        var resGrande = service.checkout(itensPadrao, CustomerTier.BASIC, false, "NORTE", 6.0, null, TODAY, null);
        assertEquals(80.00, resGrande.shipping);
        assertEquals(292.00, resGrande.total);
    }

    @Test
    @DisplayName("Frete: Deve calcular frete para Outras Regiões (fixo 40)")
    void deveCalcularFreteRegiaoOutra() {
        var resNula = service.checkout(itensPadrao, CustomerTier.BASIC, false, null, 3.0, null, TODAY, null);
        assertEquals(40.00, resNula.shipping);
        assertEquals(252.00, resNula.total);
        var resNordeste = service.checkout(itensPadrao, CustomerTier.BASIC, false, "NORDESTE", 3.0, null, TODAY, null);
        assertEquals(40.00, resNordeste.shipping);
        assertEquals(252.00, resNordeste.total);
    }
    
    @Test
    @DisplayName("Frete: Deve calcular frete para Região SUL (faixa > 5kg)")
    void deveCalcularFreteRegiaoSulFaixaGrande() {
        var resGrande = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUDESTE", 7.0, null, TODAY, null);
        assertEquals(50.00, resGrande.shipping);
        assertEquals(262.00, resGrande.total);
    }
    
    @Test
    @DisplayName("Imposto: Não deve aplicar imposto se houver apenas BOOKS")
    void deveCalcularImpostoCorretamenteApenasBook() {
        var res = service.checkout(apenasBook, CustomerTier.BASIC, false, "SUL", 3.0, null, TODAY, null);
        assertEquals(0.00, res.tax);
        assertEquals(135.00, res.total);
    }

    @Test
    @DisplayName("Lógica: Deve calcular corretamente se carrinho estiver vazio")
    void deveRetornarZeroSeCarrinhoVazio() {
        var res = service.checkout(List.of(), CustomerTier.BASIC, false, "SUL", 0.0, null, TODAY, null);
        
        assertEquals(0.00, res.subtotal);
        assertEquals(0.00, res.discountValue);
        assertEquals(0.00, res.tax);
        assertEquals(20.00, res.shipping);
        assertEquals(20.00, res.total);
    }

    @Test
    @DisplayName("Cupom: Deve aplicar DESC20 (com data de validade explícita)")
    void deveAplicarCupomDesc20ComDataValida() {
        LocalDate dataExpiracaoValida = TODAY.plusDays(1);
        var res = service.checkout(itensPadrao, CustomerTier.BASIC, false, "SUL", 3.0, "DESC20", TODAY, dataExpiracaoValida);
        
        assertEquals(40.00, res.discountValue);
        assertEquals(204.60, res.total);
    }
}