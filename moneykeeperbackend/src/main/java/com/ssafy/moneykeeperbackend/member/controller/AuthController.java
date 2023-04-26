package com.ssafy.moneykeeperbackend.member.controller;

import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.moneykeeperbackend.member.dto.common.MemberDto;
import com.ssafy.moneykeeperbackend.member.dto.common.TokenDto;
import com.ssafy.moneykeeperbackend.member.dto.response.MemberResponse;
import com.ssafy.moneykeeperbackend.member.entity.Member;
import com.ssafy.moneykeeperbackend.member.repository.MemberRepository;
import com.ssafy.moneykeeperbackend.member.service.AuthService;
import com.ssafy.moneykeeperbackend.security.TokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private PasswordEncoder passwordEncoder;

	private final MemberRepository memberRepository;

	private final AuthService authService;

	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	private final TokenProvider tokenProvider;

	/*
	 * 카카오 로그인
	 *
	 * @date 2023.04.26
	 * @author 정민지
	 * */
	@PostMapping("/kakao/callback")
	public ResponseEntity<?> getKakao(@RequestBody Map<String, String> code, HttpServletRequest request,
		HttpServletResponse response) {
		MemberDto memberDto = authService.getKakao(code.get("code"));
		authorize(memberDto.getEmail(), memberDto.getPassword(), request, response);

		MemberResponse memberResponse = MemberResponse.builder()
			.id(memberDto.getId())
			.email(memberDto.getEmail())
			.nickname(memberDto.getNickname())
			.build();
		return new ResponseEntity<>(memberResponse, HttpStatus.OK);
	}

	/*
	 * 로그인 시 권한 정보 확인
	 * @tip UsernamePasswordAuthenticationToken 에는 원본 비밀번호로 입력해야 한다.
	 *
	 * @date 2023.04.26
	 * @author 정민지
	 * */
	public TokenDto authorize(String email, String password, HttpServletRequest request,
		HttpServletResponse response) {

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email,
			password);

		Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		String authorities = getAuthorities(authentication);

		return tokenProvider.createToken(email, authorities, request, response);
	}

	public String getAuthorities(Authentication authentication) {
		return authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));
	}

}