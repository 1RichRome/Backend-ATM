package com.bn.atmcontratos.security;
import com.bn.atmcontratos.repository.UsuarioRepository; import lombok.RequiredArgsConstructor; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor public class CustomUserDetailsService implements UserDetailsService{
 private final UsuarioRepository repo; public UserDetails loadUserByUsername(String u){return repo.findByUsername(u).orElseThrow(()->new UsernameNotFoundException("Usuario no encontrado"));}
}
