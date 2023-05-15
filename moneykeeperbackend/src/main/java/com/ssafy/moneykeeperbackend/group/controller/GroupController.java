package com.ssafy.moneykeeperbackend.group.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.moneykeeperbackend.group.dto.request.GroupRequest;
import com.ssafy.moneykeeperbackend.group.service.GroupService;
import com.ssafy.moneykeeperbackend.security.userDetail.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/group")
public class GroupController {

	private final GroupService groupService;

	/*
	 * 그룹 조회
	 *
	 * @date 2023.05.15
	 * @author 정민지
	 * */
	@GetMapping("/{groupId}/{year}/{month}")
	public ResponseEntity<?> getGroupInfo(@PathVariable Long groupId, @PathVariable int year, @PathVariable int month) {
		return new ResponseEntity<>(groupService.getGroupInfo(groupId, year, month),
			HttpStatus.OK);
	}

	/*
	 * 로그인한 멤버가 가입된 그룹 전체 조회
	 *
	 * @date 2023.05.15
	 * @author 정민지
	 * */
	@GetMapping()
	public ResponseEntity<?> getAllMembersGroup(@AuthenticationPrincipal CustomUserDetails member) {
		return new ResponseEntity<>(groupService.getAllMembersGroup(member.getMember()),
			HttpStatus.OK);
	}

	/*
	 * 그룹 생성
	 *
	 * @date 2023.05.15
	 * @author 정민지
	 * */
	@PostMapping()
	public ResponseEntity<?> addGroup(@AuthenticationPrincipal CustomUserDetails member,
		@RequestBody GroupRequest groupRequest) {
		return new ResponseEntity<>(groupService.addGroup(groupRequest, member.getMember()),
			HttpStatus.OK);
	}

	/*
	 * 그룹 탈퇴
	 *
	 * @date 2023.05.15
	 * @author 정민지
	 * */
	@DeleteMapping("/secession")
	public ResponseEntity<?> deleteMemberGroup(@AuthenticationPrincipal CustomUserDetails member,
		@RequestParam Long groupId) {
		groupService.deleteMemberGroup(groupId, member.getMember());
		return new ResponseEntity<>(HttpStatus.OK);
	}

	/*
	 * 그룹 삭제
	 *
	 * @date 2023.05.15
	 * @author 정민지
	 * */
	@DeleteMapping()
	public ResponseEntity<?> deleteGroup(@AuthenticationPrincipal CustomUserDetails member,
		@RequestParam Long groupId) {
		groupService.deleteGroup(groupId, member.getMember());
		return new ResponseEntity<>(HttpStatus.OK);
	}

}