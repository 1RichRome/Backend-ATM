package com.bn.atmcontratos.security;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys; import org.springframework.beans.factory.annotation.Value; import org.springframework.security.core.userdetails.UserDetails; import org.springframework.stereotype.Service;
import javax.crypto.SecretKey; import java.nio.charset.StandardCharsets; import java.util.*;
@Service public class JwtService{
 @Value("${app.jwt.secret}") String secret; @Value("${app.jwt.expiration-ms}") long exp;
 SecretKey key(){return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));}
 public String generate(UserDetails u, Map<String,Object> claims){Date now=new Date(); return Jwts.builder().claims(claims).subject(u.getUsername()).issuedAt(now).expiration(new Date(now.getTime()+exp)).signWith(key()).compact();}
 public String username(String token){return claims(token).getSubject();}
 public boolean valid(String token, UserDetails u){return username(token).equals(u.getUsername()) && claims(token).getExpiration().after(new Date());}
 Claims claims(String token){return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();}
}
