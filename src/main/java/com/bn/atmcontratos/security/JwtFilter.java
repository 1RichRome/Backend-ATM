package com.bn.atmcontratos.security;
import jakarta.servlet.*; import jakarta.servlet.http.*; import lombok.RequiredArgsConstructor; import org.springframework.security.authentication.UsernamePasswordAuthenticationToken; import org.springframework.security.core.context.SecurityContextHolder; import org.springframework.security.core.userdetails.UserDetails; import org.springframework.security.web.authentication.WebAuthenticationDetailsSource; import org.springframework.stereotype.Component; import org.springframework.web.filter.OncePerRequestFilter; import java.io.IOException;
@Component @RequiredArgsConstructor public class JwtFilter extends OncePerRequestFilter{
 private final JwtService jwt; private final CustomUserDetailsService uds;
 protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String h=req.getHeader("Authorization"); if(h==null||!h.startsWith("Bearer ")){chain.doFilter(req,res); return;}
  String token=h.substring(7); String u=jwt.username(token);
  if(u!=null && SecurityContextHolder.getContext().getAuthentication()==null){UserDetails d=uds.loadUserByUsername(u); if(jwt.valid(token,d)){var a=new UsernamePasswordAuthenticationToken(d,null,d.getAuthorities()); a.setDetails(new WebAuthenticationDetailsSource().buildDetails(req)); SecurityContextHolder.getContext().setAuthentication(a);}}
  chain.doFilter(req,res);
 }
}
