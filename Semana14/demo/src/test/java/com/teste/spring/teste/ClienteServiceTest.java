package com.teste.spring.teste;


import com.teste.spring.teste.model.Cliente;
import com.teste.spring.teste.exception.*;
import com.teste.spring.teste.repository.ClienteRepository;
import com.teste.spring.teste.service.ClienteService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    ClienteRepository repo;

    @InjectMocks
    ClienteService service;

    @Test
    void criar_deveLancarSeEmailJaExiste() {
        Cliente c = new Cliente();
        c.setNome("João");
        c.setEmail("j@ex.com");
        when(repo.existsByEmail("j@ex.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(c))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email já cadastrado");
        verify(repo, never()).save(any());
    }

    @Test
    void atualizar_deveAtualizarCamposBasicos() {
        Cliente antigo = new Cliente();
        antigo.setId(1L);
        antigo.setNome("Antigo");
        antigo.setEmail("a@ex.com");
        antigo.setTelefone("11");

        when(repo.findById(1L)).thenReturn(Optional.of(antigo));
        when(repo.findByEmail("novo@ex.com")).thenReturn(Optional.of(antigo)); // mesmo cliente
        when(repo.existsByEmail("novo@ex.com")).thenReturn(true);
        when(repo.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));

        Cliente dados = new Cliente();
        dados.setNome("Novo");
        dados.setEmail("novo@ex.com");
        dados.setTelefone("22");

        Cliente atualizado = service.atualizar(1L, dados);

        assertThat(atualizado.getNome()).isEqualTo("Novo");
        assertThat(atualizado.getEmail()).isEqualTo("novo@ex.com");
        assertThat(atualizado.getTelefone()).isEqualTo("22");
    }


    @Test
    @DisplayName("Service: Buscar deve retornar cliente ou lançar NotFoundException")
    void buscar_deveRetornarClienteOuLancarExcecao() {
        // Cenário 1: Encontrado
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Cliente Teste");
        when(repo.findById(1L)).thenReturn(Optional.of(c));

        Cliente encontrado = service.buscar(1L);
        assertThat(encontrado.getNome()).isEqualTo("Cliente Teste");

        // Cenário 2: Não Encontrado
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Cliente não encontrado");
    }

    @Test
    @DisplayName("Service: Excluir deve chamar delete no repositório")
    void excluir_deveChamarDelete() {
        Cliente c = new Cliente();
        c.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(c));

        // Executa o método
        service.excluir(1L);

        // Verifica se o repo.delete(c) foi chamado exatamente 1 vez
        verify(repo, times(1)).delete(c);
    }

    @Test
    @DisplayName("Service: Atualizar deve lançar BusinessException se email já for de outro cliente")
    void atualizar_deveLancarExcecaoSeEmailJaExisteEmOutro() {
        // Cliente que queremos atualizar (ID 1)
        Cliente clienteExistente = new Cliente();
        clienteExistente.setId(1L);
        clienteExistente.setEmail("antigo@ex.com");

        // Cliente que já possui o email que queremos usar (ID 2)
        Cliente outroCliente = new Cliente();
        outroCliente.setId(2L);
        outroCliente.setEmail("novo@ex.com");

        // Dados da atualização (queremos mudar o email do ID 1 para 'novo@ex.com')
        Cliente dadosAtualizacao = new Cliente();
        dadosAtualizacao.setEmail("novo@ex.com");
        dadosAtualizacao.setNome("Nome Novo");

        // Configuração do Mock
        when(repo.findById(1L)).thenReturn(Optional.of(clienteExistente));
        when(repo.existsByEmail("novo@ex.com")).thenReturn(true);
        when(repo.findByEmail("novo@ex.com")).thenReturn(Optional.of(outroCliente)); // Email pertence ao ID 2

        // Teste
        assertThatThrownBy(() -> service.atualizar(1L, dadosAtualizacao))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email já cadastrado para outro cliente");
    }
}
