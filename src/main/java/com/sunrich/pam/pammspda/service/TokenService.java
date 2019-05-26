package com.sunrich.pam.pammspda.service;

import com.sunrich.pam.common.domain.Token;
import com.sunrich.pam.common.exception.ErrorCodes;
import com.sunrich.pam.common.exception.UnauthorizedException;
import com.sunrich.pam.pammspda.repository.TokenRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TokenService {
  private final TokenRepository tokenRepository;

  public TokenService(TokenRepository tokenRepository) {
    this.tokenRepository = tokenRepository;
  }

  public Token findEntityByToken(String token) throws UnauthorizedException {
    Optional<Token> tokenOptional = tokenRepository.findByToken(token);
    if (!tokenOptional.isPresent()) {
      throw new UnauthorizedException(ErrorCodes.INVALID_TOKEN, "Invalid Token");
    }
    return tokenOptional.get();
  }
}
