<template>
  <view class="agreement-page">
    <view class="agreement-shell">
      <view class="agreement-hero">
        <view class="hero-badge">艺本艺术 · 服务公示</view>
        <text class="agreement-title">{{ agreementTitle }}</text>
        <text class="agreement-date">更新日期：{{ agreementUpdatedAt }}</text>
        <text class="agreement-desc">{{ agreementIntro }}</text>
        <view class="agreement-meta">
          <view class="meta-pill" v-for="item in agreementMeta" :key="item">
            <text>{{ item }}</text>
          </view>
        </view>
      </view>

      <view v-if="normalizedAgreementType === 'user' || normalizedAgreementType === 'privacy'" class="hero-summary">
        <view class="hero-summary-card">
          <text class="hero-summary-label">适用范围</text>
          <text class="hero-summary-value">平台公开服务与核心交易场景</text>
        </view>
        <view class="hero-summary-card">
          <text class="hero-summary-label">访问入口</text>
          <text class="hero-summary-value">H5、小程序、客户端与相关页面</text>
        </view>
        <view class="hero-summary-card highlight">
          <text class="hero-summary-label">备案信息</text>
          <text class="hero-summary-value">浙ICP备2026041466号-1</text>
        </view>
      </view>
    </view>

    <view class="agreement-content card">
      <!-- 用户协议内容 -->
      <template v-if="agreementType === 'user'">
        <view class="content-section">
          <view class="section-title">一、协议的适用与确认</view>
          <view class="section-content">
            1. 本《用户服务协议》适用于您访问、注册、登录、浏览、发布、购买、出售、委托、分享或以其他方式使用艺本艺术平台及相关网站、小程序、H5 页面、客户端和配套服务的全部过程。<br>
            2. {{ userScopeParagraph }}<br>
            3. 您在注册、登录、勾选同意、点击确认、下单支付或实际使用平台服务前，应当认真阅读本协议及平台不时公布的规则、声明、说明、提示、特别约定和单项服务条款。上述内容一经发布，即成为本协议不可分割的组成部分。<br>
            4. 当您完成注册或使用平台服务时，即表示您已充分理解并同意接受本协议全部内容；如您不同意本协议任何条款，您应立即停止注册或使用平台服务。<br>
            5. 如您为无民事行为能力人或限制民事行为能力人，应在监护人同意、指导下使用平台；涉及购买、出售、认证、结算等服务的，原则上应由具备完全民事行为能力的主体完成。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、账户注册、登录与安全</view>
          <view class="section-content">
            1. 您在平台注册时，应提供真实、准确、完整且持续有效的注册资料，并在资料发生变化时及时更新。因您提供的信息不真实、不准确、不完整或更新不及时导致的任何后果，由您自行承担。<br>
            2. 您应妥善保管账号、密码、验证码、第三方登录授权信息及设备，不得出借、出租、转让、售卖或以其他方式许可他人使用您的账号。<br>
            3. 平台有权根据法律法规、监管要求、风控需要或业务场景，对部分账号启用实名认证、手机号验证、人脸核验、银行卡校验、支付账户核验或其他身份验证措施。<br>
            4. 您发现账号存在异常登录、盗用、冒用或其他安全风险时，应立即通知平台并采取修改密码、终止授权等措施。平台将尽合理努力协助处理，但对在您通知前已经发生的指令、交易或操作，不当然承担责任。<br>
            5. 一个用户只能基于真实身份开立和使用符合平台规则的账号。对于恶意注册、批量注册、刷号、伪造身份、冒名顶替或规避风控的账号，平台有权采取限制功能、暂停服务、注销账号等措施。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、平台服务内容与规则变更</view>
          <view class="section-content">
            1. {{ platformServiceParagraph }}<br>
            2. 平台有权基于业务发展、法律法规、监管政策、风控安全、系统维护或用户体验，对服务功能、页面布局、收费方式、开放条件、结算规则、活动规则、交易流程和合作模式进行调整。<br>
            3. 如相关变更会对您的核心权利义务产生重大影响，平台将通过页面提示、站内信、公告、弹窗或其他合理方式通知您。您在变更生效后继续使用服务的，视为接受更新后的规则。<br>
            4. 平台将尽力保障服务连续性，但不保证服务始终无中断、无故障或完全适配所有设备、网络、操作系统及浏览器环境。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、用户行为规范</view>
          <view class="section-content">
            1. 您在使用平台服务时，应遵守中华人民共和国法律法规、公序良俗及平台各项规则，不得利用平台从事违法违规、侵权、欺诈、扰乱交易秩序或损害他人合法权益的行为。<br>
            2. 您不得发布、上传、传播、存储或展示以下内容：违法内容，虚假交易信息，侵害他人知识产权或人格权的内容，淫秽低俗内容，侮辱诽谤内容，恶意营销信息，计算机病毒或其他可能危害系统安全的信息。<br>
            3. 您不得实施刷单、炒信、虚构交易、恶意下单、恶意退款、恶意差评、绕过平台私下成交、批量爬取数据、破解接口、干扰系统、冒充平台人员、滥用补贴或优惠等行为。<br>
            4. 如您发布内容、评论、作品资料、头像、昵称、简介、认证信息或其他材料，您应保证其真实、合法、准确，不侵犯第三方合法权益，并自行承担相应责任。<br>
            5. 平台有权依据法律法规和平台规则，对疑似违规内容和行为进行审核、监测、拦截、删除、屏蔽、限流、下架、冻结、终止服务或追究责任。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、交易规则、订单履约与风险提示</view>
          <view class="section-content">
            1. {{ transactionRiskParagraph }}<br>
            2. {{ orderNoticeParagraph }}<br>
            3. 艺术品具有较强的个体性、审美差异性和非标准化特征，您应理解艺术品在色彩、笔触、材质、装框、尺寸、手工痕迹、年代感与展示环境上可能存在合理差异。除法律另有规定或平台另有承诺外，部分艺术品交易可能不适用无理由退换。<br>
            4. {{ specialGoodsParagraph }}<br>
            5. 买卖双方应依约履行付款、发货、签收、验收、开票、售后协商等义务。若任何一方存在违约、争议、异常交易或涉嫌欺诈，平台有权基于风控和争议处理需要暂缓结算、限制提现、要求补充材料或采取其他必要措施。<br>
            6. 平台不是所有交易的买卖一方。除页面明确标识为平台自营外，平台通常作为网络服务提供者、信息展示者和交易秩序维护者，为交易双方提供信息发布、撮合、工具支持和辅助履约服务。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、知识产权与内容授权</view>
          <view class="section-content">
            1. 平台上展示的商标、名称、域名、页面设计、版面结构、文字内容、图片素材、音视频、程序代码、数据资料及其他内容，其知识产权归平台或相应权利人所有。未经权利人事先书面许可，任何人不得擅自使用。<br>
            2. 您上传、发布、提交或展示于平台的作品图片、头像、昵称、简介、评论、问答、文章、视频及其他内容，应确保拥有合法权利或已取得充分授权。因该等内容引发的投诉、索赔、行政处罚或诉讼，由您自行承担责任。<br>
            3. 在为实现平台展示、宣传推广、交易撮合、证书生成、内容分发、技术处理、纠纷处理、数据备份及服务履约所必需的范围内，您授予平台一项全球范围内、非独占、可分许可的使用许可。该授权不改变您对原始内容依法享有的权利归属。<br>
            4. 平台尊重知识产权。如任何主体认为平台内容侵犯其合法权益，可按平台公示方式提交权利通知及证明材料，平台将在核实后依法依规处理。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">七、费用、发票、结算与税务</view>
          <view class="section-content">
            1. 平台部分服务可能收费，具体收费项目、标准、结算方式、支付时间及优惠活动以页面展示、订单信息或专项协议为准。<br>
            2. 您应按约支付货款、保证金、服务费、技术服务费、物流费或其他约定费用。若因支付失败、账户异常、余额不足或拒付导致交易不能继续，由相关责任方自行承担后果。<br>
            3. 涉及艺术家、卖家、推广者或其他合作角色的收益结算时，平台可依据订单完成状态、售后期、争议状态、风控审查、发票要求、税务合规要求等决定具体结算时点。<br>
            4. 您应依法履行与交易、经营、推广或结算相关的纳税义务；法律法规要求平台代扣代缴的，平台有权依法处理。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">八、免责声明与责任限制</view>
          <view class="section-content">
            1. 平台将尽合理努力保障服务安全、稳定、持续，但对于因不可抗力、网络故障、第三方服务异常、黑客攻击、系统升级、监管要求、通信线路故障、电力中断或其他非平台可控原因导致的服务中断、迟延、数据异常或损失，平台在法律允许范围内承担相应责任。<br>
            2. 平台不对用户之间、用户与第三方之间因交易、侵权、违约、质量争议、物流争议、税务争议、知识产权争议等产生的全部风险作绝对担保，但会根据平台规则提供必要协助。<br>
            3. 在法律允许的范围内，平台对任何间接损失、附带损失、预期利益损失、商誉损失、数据损失不承担责任；平台承担责任的范围，以其直接因故意或重大过失给您造成的实际损失为限。<br>
            4. 若您因违反本协议、平台规则或法律法规造成平台、其他用户或第三方损失，您应承担相应赔偿责任，并使平台免责。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">九、协议变更、服务中止与终止</view>
          <view class="section-content">
            1. 平台有权根据法律法规变化、业务调整、交易安全和经营需要对本协议进行修订，修订后的内容将在相关页面公布并于公布后的合理期限内生效。<br>
            2. 若您不同意修订后的协议，应停止使用平台服务；若您在协议变更后继续使用平台服务，视为您接受修订后的协议。<br>
            3. 如您违反本协议、平台规则或法律法规，或平台有合理理由认为您的行为可能危及交易安全、用户权益、知识产权或系统安全，平台有权单方采取警告、限制功能、暂停交易、冻结账户、终止服务等措施。<br>
            4. 您可依据平台规则申请注销账号。账号注销后，平台将在法律法规允许和要求的范围内保存必要信息，已发生交易及相关权利义务不因账号注销而消灭。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">十、通知、法律适用与争议解决</view>
          <view class="section-content">
            1. 平台可通过公告、弹窗、站内信、短信、电话、电子邮件、消息推送等方式向您发送与服务有关的通知。发送至您留存联系方式或在页面成功展示的，视为送达。<br>
            2. 本协议的订立、生效、解释、履行及争议解决，均适用中华人民共和国法律。<br>
            3. 因本协议或平台服务引起的争议，双方应先友好协商；协商不成的，任一方可向平台运营主体所在地有管辖权的人民法院提起诉讼。<br>
            4. 如本协议部分条款被认定无效或不可执行，不影响其余条款的效力。平台未行使某项权利，不构成对该权利的放弃。
          </view>
        </view>
      </template>

      <!-- 隐私政策内容 -->
      <template v-else-if="agreementType === 'privacy'">
        <view class="artist-summary privacy-summary">
          <view class="summary-item">
            <text class="summary-label">收集原则</text>
            <text class="summary-value">最小必要</text>
          </view>
          <view class="summary-item">
            <text class="summary-label">敏感信息</text>
            <text class="summary-value">单独授权</text>
          </view>
          <view class="summary-item highlight">
            <text class="summary-label">用户权利</text>
            <text class="summary-value">可访问、更正、删除与撤回授权</text>
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">一、适用范围与基本原则</view>
          <view class="section-content">
            1. 本《隐私政策》适用于艺本艺术平台在您访问、注册、登录、浏览、咨询、购买、出售、认证、结算、参与活动、联系客服及使用相关产品服务过程中，对您个人信息的收集、使用、存储、共享、转让、公开披露与保护。<br>
            2. 我们遵循合法、正当、必要、诚信和最小必要原则处理个人信息，并努力做到处理目的明确、范围合理、透明可知。<br>
            3. 不同业务场景对应不同的信息处理规则；当特定功能另行通过页面授权弹窗、服务协议、补充说明告知您时，该等说明与本政策共同构成完整的信息处理规则。<br>
            4. 如您不同意本政策的部分内容，可能导致我们无法向您提供对应服务，但不影响您浏览部分公开信息。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、我们收集哪些信息</view>
          <view class="section-content">
            1. 账号与身份信息：当您注册、登录或进行实名认证时，我们可能收集手机号、验证码、昵称、头像、登录标识、真实姓名、身份证件信息、人脸核验信息及其他依法必需的身份资料。<br>
            2. 联系与履约信息：当您购买、出售、收货、开票或申请售后时，我们可能收集联系人姓名、联系电话、收货地址、发票信息、物流信息、订单备注和沟通记录。<br>
            3. {{ transactionCollectionParagraph }}<br>
            4. 艺术家与经营信息：当您申请艺术家入驻、上传作品或经营店铺时，我们可能收集艺术领域、城市、个人简介、作品图片、作品说明、创作信息、认证材料、经营数据及相关证书信息。<br>
            5. 内容与互动信息：当您收藏、关注、评论、咨询、反馈、分享或参与活动时，我们可能收集相应的操作记录、互动内容、上传素材和客服沟通信息。<br>
            6. 设备与日志信息：为保障服务稳定和安全，我们可能收集设备型号、操作系统、应用版本、IP 地址、接入网络、设备标识、日志信息、浏览记录、点击记录、搜索记录和异常崩溃信息。<br>
            7. 敏感个人信息：身份证件、人脸信息、银行卡号、收款账号、精确联系地址等属于敏感个人信息。我们仅在必要场景下处理，并通过单独提示、授权或页面确认等方式征得您的必要同意。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、我们如何使用您的信息</view>
          <view class="section-content">
            1. 用于为您提供账号注册登录、身份校验、作品展示、订单创建、支付结算、物流协助、证书查看、售后处理、消息通知和客服支持。<br>
            2. 用于维护交易安全与平台秩序，包括身份识别、风险评估、反欺诈、反洗钱、异常交易监测、争议处理和违规行为处置。<br>
            3. 用于优化产品体验，包括页面展示优化、功能调试、故障排查、系统维护、服务质量评估和运营统计分析。我们在可行范围内优先采用去标识化、匿名化方式。<br>
            4. 用于向您发送与订单履约、账号安全、服务变更、活动参与有关的通知；若涉及营销推广、商业信息推送或个性化推荐，我们会依法提供关闭、拒收或退订方式。<br>
            5. 在符合法律法规要求的前提下，我们可能基于自动化决策向您展示排序结果、推荐内容或风险校验结果；如该等决策对您权益有重大影响，您可通过客服与我们联系。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、我们如何使用 Cookie、SDK 与同类技术</view>
          <view class="section-content">
            1. 为保障登录状态、提高访问效率、进行安全防护、统计分析和功能实现，我们可能使用 Cookie、本地存储、消息推送、日志采集、设备标识和其他同类技术。<br>
            2. 我们接入的第三方 SDK、接口或插件可能因支付、登录、实名认证、地图定位、消息通知、文件存储、客服等功能需要而获取必要信息。我们会要求合作方遵守个人信息保护义务，并尽力评估其安全性。<br>
            3. 您可通过设备或浏览器设置管理部分 Cookie、通知权限、相册权限、摄像头权限、麦克风权限、定位权限或系统授权，但关闭相关权限后可能影响对应功能正常使用。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、信息共享、转让、公开披露</view>
          <view class="section-content">
            1. 我们不会向无关第三方共享您的个人信息，除非已取得您的单独同意，或法律法规另有规定。<br>
            2. 为实现支付、物流、实名认证、云存储、短信通知、客服支持、风控审核、争议处理等服务，我们可能向必要合作方共享完成服务所需的最小范围信息。<br>
            3. 在公司合并、分立、重组、资产转让或其他类似交易中，如涉及个人信息转移，我们将要求新的持有方继续受本政策约束；如处理目的、方式发生变化，我们将依法重新征求您的同意。<br>
            4. 我们仅会在法律法规要求、响应行政司法机关依法提出的要求、维护重大公共利益，或保护我们及其他用户的人身财产安全所必需的情况下，依法公开披露相关信息。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、信息存储与安全保护</view>
          <view class="section-content">
            1. 我们会采取与业务规模相适应的安全技术和管理措施，包括但不限于访问控制、身份鉴别、加密传输、去标识化处理、日志审计、数据备份和安全监测，以保护您的个人信息。<br>
            2. 对身份证件、人脸核验信息、支付账户信息等敏感个人信息，我们将采取更严格的权限管理和保护措施。<br>
            3. 我们仅在实现本政策所述目的所必需的最短期限内保存您的个人信息；法律法规、监管要求、争议处理、会计审计或履行法定义务需要更长保存期限的除外。<br>
            4. 互联网并非绝对安全的环境。如发生或可能发生个人信息安全事件，我们将按照法律法规要求及时采取补救措施，并以合理可行的方式告知您事件情况、影响范围和建议措施。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">七、您的权利</view>
          <view class="section-content">
            1. 您有权访问、查阅、更正、补充您的个人信息，并可在平台设置页、个人资料页、地址管理页、实名认证页等自助管理相关信息。<br>
            2. 在符合法律法规规定的情形下，您有权要求删除个人信息、撤回同意、注销账号、获取个人信息副本，或对自动化决策提出解释和异议。<br>
            3. 当您撤回授权或关闭权限后，我们将不再处理相应信息，但此前基于您授权开展的处理活动不受影响；同时，撤回授权可能导致部分服务无法继续使用。<br>
            4. 如您需要行使相关权利，可通过平台客服、意见反馈、站内联系入口或其他页面公示方式与我们联系，我们将在法律法规规定期限内响应。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">八、未成年人保护</view>
          <view class="section-content">
            1. 我们主要面向成年人提供交易、认证和经营相关服务。未成年人应在监护人同意和指导下使用平台。<br>
            2. 如我们发现存在未经监护人同意收集未成年人个人信息的情形，将依法尽快删除或采取必要处理措施。<br>
            3. 监护人如对未成年人个人信息保护有疑问，可通过平台客服或页面公示方式与我们联系。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">九、政策更新与联系我们</view>
          <view class="section-content">
            1. 因业务变化、法律法规更新或监管要求，我们可能适时修订本政策。若修订内容对您的权利义务有重大影响，我们会通过弹窗、公告、站内信或其他合理方式提示。<br>
            2. 更新后的政策自公布或通知载明的生效日期起实施。您继续使用平台服务的，视为已阅读并接受更新后的隐私政策。<br>
            3. 如您对本政策或个人信息保护事宜有任何疑问、意见或投诉建议，可通过平台客服、意见反馈页面或平台公示联系方式与我们联系。
          </view>
        </view>
      </template>

      <!-- 艺术家入驻协议 -->
      <template v-else-if="agreementType === 'artist'">
        <view class="artist-summary">
          <view class="summary-item">
            <text class="summary-label">年度服务费</text>
            <text class="summary-value">¥3,600 / 年</text>
          </view>
          <view class="summary-item highlight">
            <text class="summary-label">当前阶段权益</text>
            <text class="summary-value">邀请好友赠送年费</text>
            <text class="summary-note">平台服务分成按交易条款执行，详见下方费用与分成说明。</text>
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">一、认证与入驻条件</view>
          <view class="section-content">
            1. 申请人须为原创艺术作品的创作者。<br>
            2. 申请人须年满18周岁，具有完全民事行为能力。<br>
            3. 申请人需提供真实有效的身份证明材料。<br>
            4. 申请人需提供20件本人原创代表作品，供平台进行风格、质量与持续创作能力审核。<br>
            5. 申请人需遵守平台规则，不从事违法违规活动。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、申请流程</view>
          <view class="section-content">
            1. 阅读认证指南，确认年费、平台服务分成和作品提交要求。<br>
            2. 录入真实个人信息，包括真实姓名、身份证号、艺术领域、所在城市、联系方式和个人简介。<br>
            3. 上传20件本人代表作品及必要的身份证明材料。<br>
            4. 平台将在提交后进行资料核验、作品审核和风险评估，并通过站内通知或联系方式反馈结果。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、费用与分成</view>
          <view class="section-content">
            1. 艺术家认证年度服务费为人民币3,600元/年。<br>
            2. 当前推广阶段，艺术家分享平台并邀请好友完成注册，可获得平台赠送的3,600元年度服务费权益。具体领取规则以页面展示及平台通知为准。<br>
            3. <text class="muted-inline">艺术家作品发生交易时，平台按每笔消费金额收取15%的平台服务分成。</text><br>
            4. 订单款项将在交易完成、售后期届满或平台确认无争议后，按照平台结算周期进行结算。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">四、权利义务</view>
          <view class="section-content">
            <strong>权利：</strong><br>
            1. 入驻艺术家享有平台提供的各项服务。<br>
            2. 入驻艺术家可参与平台组织的各类活动。<br>
            3. 入驻艺术家可获得平台提供的推广资源、艺术家主页、作品发布与数据看板等能力。<br><br>
            <strong>义务：</strong><br>
            1. 保证上传作品为本人原创，不侵犯他人权益。<br>
            2. 及时处理买家咨询和订单。<br>
            3. 保证作品质量，按时发货。<br>
            4. 遵守平台规则，维护平台秩序。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">五、作品与知识产权</view>
          <view class="section-content">
            1. 艺术家应确保提交、发布和销售的作品为本人原创或已获得合法授权。<br>
            2. 艺术家授权平台在推广、展示、交易、证书生成和服务履约所需范围内使用作品图片、作品信息和艺术家资料。<br>
            3. 如因作品权属、肖像权、著作权或其他第三方权益产生争议，由艺术家承担相应责任。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">六、违规处理</view>
          <view class="section-content">
            1. 发现违规行为，平台有权采取警告、下架、封号等措施。<br>
            2. 违规行为造成损失的，由违规方承担相应责任。<br>
            3. 艺术家有权申诉，平台将进行核实处理。
          </view>
        </view>
      </template>

      <!-- 经纪人协议 -->
      <template v-else-if="agreementType === 'promoter'">
        <view class="content-section">
          <view class="section-title">一、经纪人职责</view>
          <view class="section-content">
            1. 经纪人负责推广平台商品，分享给潜在买家。<br>
            2. 经纪人需维护平台形象，遵守推广规范。<br>
            3. 经纪人应积极引导用户完成交易。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">二、分成制度</view>
          <view class="section-content">
            1. 经纪人根据有效推广订单获得分成。<br>
            2. 分成比例为商品成交金额的5%-15%。<br>
            3. 分成按月结算，扣除相关费用后发放。<br>
            4. 经纪人等级越高，分成比例越高。
          </view>
        </view>

        <view class="content-section">
          <view class="section-title">三、违规处理</view>
          <view class="section-content">
            1. 严禁使用虚假推广、刷单等违规手段。<br>
            2. 严禁传播违法、有害信息。<br>
            3. 违规者将被取消经纪人资格并追究责任。
          </view>
        </view>
      </template>
    </view>

    <view class="agreement-footer">
      <view class="agreement-footer-grid">
        <view class="agreement-footer-column">
          <text class="footer-heading">平台服务</text>
          <text>用户协议</text>
          <text>隐私政策</text>
          <text>服务信息公示</text>
        </view>
        <view class="agreement-footer-column">
          <text class="footer-heading">帮助支持</text>
          <text>客服热线：400-888-8888</text>
          <text>联系邮箱：service@shiyiju.com</text>
          <text>联系地址：杭州市余杭区景兴路896号</text>
        </view>
        <view class="agreement-footer-column">
          <text class="footer-heading">备案与合规</text>
          <text>网站备案：浙ICP备2026041466号-1</text>
          <text>备案主体：孟儒（个人）</text>
          <text>审核通过日期：2026-06-10</text>
        </view>
      </view>
      <view class="agreement-footer-bar">
        <text>© 2026 艺本艺术。保留所有权利。</text>
        <text>中国大陆</text>
      </view>
    </view>
  </view>
</template>

<script>
import { AUCTION_ENABLED } from '@/utils/platform.js'

export default {
  data() {
    return {
      agreementType: 'user'
    }
  },

  computed: {
    normalizedAgreementType() {
      const aliasMap = {
        terms: 'user',
        user: 'user',
        privacy: 'privacy',
        artist: 'artist',
        artist_cert: 'artist',
        promoter: 'promoter',
        withdraw: 'promoter'
      }

      return aliasMap[this.agreementType] || 'user'
    },

    agreementTitle() {
      const titles = {
        user: '用户服务协议',
        privacy: '隐私政策',
        artist: '艺术家入驻协议',
        promoter: '经纪人协议'
      }
      return titles[this.normalizedAgreementType] || '用户服务协议'
    },

    agreementUpdatedAt() {
      const dates = {
        user: '2026年6月24日',
        privacy: '2026年6月24日',
        artist: '2026年6月24日',
        promoter: '2026年6月24日'
      }

      return dates[this.normalizedAgreementType] || '2026年6月24日'
    },

    agreementIntro() {
      const intros = {
        user: '说明平台服务范围、交易规则、知识产权、责任边界与争议解决方式。',
        privacy: '说明个人信息在账号、交易、履约、客服和安全风控等场景中的处理规则。',
        artist: '说明艺术家入驻条件、费用分成、权利义务与作品授权范围。',
        promoter: '说明经纪人推广职责、分成制度与违规处理方式。'
      }

      return intros[this.normalizedAgreementType] || intros.user
    },

    agreementMeta() {
      const metaMap = {
        user: ['服务规则', '交易说明', '权责边界'],
        privacy: ['最小必要', '敏感信息保护', '用户权利'],
        artist: ['入驻条件', '费用分成', '作品授权'],
        promoter: ['推广职责', '分成制度', '违规处理']
      }

      return metaMap[this.normalizedAgreementType] || metaMap.user
    },

    userScopeParagraph() {
      if (AUCTION_ENABLED) {
        return '艺本艺术平台的运营主体为艺本艺术相关经营主体。平台根据实际业务安排提供艺术作品展示、艺术家入驻、作品交易、拍卖竞价、支付结算、物流协助、证书展示、售后协商、内容发布及其他延伸服务。'
      }
      return '艺本艺术平台的运营主体为艺本艺术相关经营主体。当前小程序版本主要提供艺术作品展示、艺术家入驻、作品交易、支付结算、物流协助、证书展示、售后协商、内容发布及其他延伸服务。'
    },

    platformServiceParagraph() {
      if (AUCTION_ENABLED) {
        return '平台目前及未来可能向您提供包括但不限于作品浏览、艺术家主页、收藏关注、发布分享、在线咨询、订单管理、交易支付、发票申请、收货管理、积分优惠、拍卖竞价、证书查看、推广合作等服务。具体服务内容以页面实际展示和平台公告为准。'
      }
      return '平台目前及未来可能向您提供包括但不限于作品浏览、艺术家主页、收藏关注、发布分享、在线咨询、订单管理、交易支付、发票申请、收货管理、积分优惠、证书查看、推广合作等服务。具体服务内容以页面实际展示和平台公告为准。'
    },

    transactionRiskParagraph() {
      if (AUCTION_ENABLED) {
        return '平台中的艺术作品、衍生品、拍卖标的或相关服务信息由对应商家、艺术家、卖家或合作方发布，平台会尽合理努力进行审核与管理，但页面信息、库存状态、价格、交付周期、材质尺寸、装裱状态、色差观感、作者说明等仍可能因实际情况存在差异。'
      }
      return '平台中的艺术作品、衍生品或相关服务信息由对应商家、艺术家、卖家或合作方发布，平台会尽合理努力进行审核与管理，但页面信息、库存状态、价格、交付周期、材质尺寸、装裱状态、色差观感、作者说明等仍可能因实际情况存在差异。'
    },

    orderNoticeParagraph() {
      if (AUCTION_ENABLED) {
        return '您在下单、竞拍、支付定金、支付货款、申请结算或确认收货前，应自行充分了解作品信息、交易规则、售后政策、竞拍节奏、配送限制和税费承担方式。'
      }
      return '您在下单、支付货款、申请结算或确认收货前，应自行充分了解作品信息、交易规则、售后政策、配送限制和税费承担方式。'
    },

    specialGoodsParagraph() {
      if (AUCTION_ENABLED) {
        return '对于拍卖、限量发售、定制作品、艺术家直售、证书绑定或其他特殊商品/服务，平台可根据具体场景设置不同的保证金规则、成交规则、违约责任、结算周期和售后限制，届时以页面说明及单项协议为准。'
      }
      return '对于限量发售、定制作品、艺术家直售、证书绑定或其他特殊商品/服务，平台可根据具体场景设置不同的成交规则、违约责任、结算周期和售后限制，届时以页面说明及单项协议为准。'
    },

    transactionCollectionParagraph() {
      if (AUCTION_ENABLED) {
        return '交易与结算信息：当您下单、支付、提现、参与拍卖、申请结算或退款时，我们可能收集订单信息、支付状态、支付渠道标识、交易流水、保证金记录、银行卡或收款账户信息。'
      }
      return '交易与结算信息：当您下单、支付、提现、申请结算或退款时，我们可能收集订单信息、支付状态、支付渠道标识、交易流水、银行卡或收款账户信息。'
    }
  },

  onLoad(options) {
    if (options.type) {
      this.agreementType = options.type
    }
    this.agreementType = this.normalizedAgreementType
  }
}
</script>

<style lang="scss" scoped>
.agreement-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(212, 175, 55, 0.14), transparent 28%),
    linear-gradient(180deg, #fbf7f1 0%, #f4ede2 54%, #efe4d4 100%);
  padding: 24rpx 24rpx 44rpx;
  box-sizing: border-box;
}

.agreement-shell {
  max-width: 1120rpx;
  margin: 0 auto 26rpx;
}

.agreement-hero {
  position: relative;
  overflow: hidden;
  padding: 42rpx 34rpx 34rpx;
  border-radius: 32rpx;
  background:
    linear-gradient(180deg, rgba(11, 14, 18, 0.04), rgba(11, 14, 18, 0.18)),
    radial-gradient(circle at top right, rgba(217, 167, 89, 0.88), transparent 30%),
    linear-gradient(135deg, #1d2329, #314840 54%, #805a26 100%);
  box-shadow: 0 24rpx 54rpx rgba(75, 43, 20, 0.16);

  &::before {
    content: '';
    position: absolute;
    width: 260rpx;
    height: 260rpx;
    border-radius: 50%;
    left: -88rpx;
    top: -88rpx;
    background: rgba(255, 248, 235, 0.1);
  }
}

.hero-badge,
.agreement-title,
.agreement-date,
.agreement-desc,
.agreement-meta {
  position: relative;
  z-index: 1;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  min-height: 54rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 248, 235, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.18);
  color: #fff3df;
  font-size: 22rpx;
  letter-spacing: 0.08em;
}

.agreement-title {
  display: block;
  font-size: 50rpx;
  line-height: 60rpx;
  font-weight: 800;
  color: #fff8ec;
  margin-top: 26rpx;
}

.agreement-date {
  display: block;
  font-size: 24rpx;
  color: rgba(255, 247, 232, 0.72);
  margin-top: 16rpx;
}

.agreement-desc {
  display: block;
  font-size: 26rpx;
  line-height: 42rpx;
  color: rgba(255, 247, 232, 0.84);
  margin-top: 18rpx;
}

.agreement-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 24rpx;
}

.meta-pill {
  padding: 12rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 248, 235, 0.14);
  border: 1rpx solid rgba(255, 255, 255, 0.14);

  text {
    color: #fff8ec;
    font-size: 22rpx;
  }
}

.hero-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16rpx;
  margin-top: 18rpx;
}

.hero-summary-card {
  padding: 22rpx;
  border-radius: 22rpx;
  background: rgba(255, 250, 243, 0.72);
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 14rpx 30rpx rgba(75, 43, 20, 0.08);

  &.highlight {
    background: rgba(255, 246, 226, 0.92);
    border-color: rgba(212, 175, 55, 0.24);
  }
}

.hero-summary-label {
  display: block;
  font-size: 22rpx;
  color: #7a6a59;
  margin-bottom: 10rpx;
}

.hero-summary-value {
  display: block;
  font-size: 28rpx;
  line-height: 38rpx;
  font-weight: 700;
  color: #2b2119;
}

.agreement-content {
  max-width: 1120rpx;
  margin: 0 auto;
  padding: 30rpx;
  border-radius: 30rpx;
  background: rgba(255, 252, 247, 0.92);
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  box-shadow: 0 18rpx 46rpx rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(12rpx);

  .artist-summary {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16rpx;
    margin-bottom: 34rpx;
  }

  .privacy-summary .summary-value {
    font-size: 26rpx;
  }

  .summary-item {
    padding: 22rpx;
    border-radius: 22rpx;
    background: rgba(247, 243, 236, 0.78);
    border: 1rpx solid rgba(32, 23, 18, 0.08);

    &.highlight {
      grid-column: span 2;
      background: #fff7e6;
      border-color: rgba(212, 175, 55, 0.28);
    }
  }

  .summary-label {
    display: block;
    font-size: 22rpx;
    color: #86909c;
    margin-bottom: 8rpx;
  }

  .summary-value {
    display: block;
    font-size: 28rpx;
    line-height: 36rpx;
    font-weight: 800;
    color: #ad8b2d;
  }

  .summary-note {
    display: block;
    margin-top: 10rpx;
    font-size: 22rpx;
    line-height: 34rpx;
    color: #8c8173;
    font-weight: 400;
  }

  .content-section {
    margin-bottom: 28rpx;
    padding-bottom: 28rpx;
    border-bottom: 1rpx solid rgba(32, 23, 18, 0.08);

    &:last-child {
      margin-bottom: 0;
      padding-bottom: 0;
      border-bottom: 0;
    }

    .section-title {
      display: inline-flex;
      align-items: center;
      min-height: 58rpx;
      padding: 0 18rpx;
      border-radius: 999rpx;
      background: rgba(212, 175, 55, 0.1);
      border: 1rpx solid rgba(212, 175, 55, 0.16);
      font-size: 28rpx;
      line-height: 42rpx;
      font-weight: 800;
      color: #382618;
      margin-bottom: 16rpx;
    }

    .section-content {
      font-size: 26rpx;
      color: #4c4238;
      line-height: 1.9;
      text-align: justify;
      padding: 8rpx 4rpx 0;

      strong {
        color: #1f2329;
        font-weight: 700;
      }

      .muted-inline {
        font-size: 24rpx;
        color: #7c6b59;
        font-weight: 400;
      }
    }
  }
}

.agreement-footer {
  max-width: 1120rpx;
  margin: 28rpx auto 0;
  padding: 30rpx 10rpx 0;
  border-top: 1rpx solid rgba(32, 23, 18, 0.08);
}

.agreement-footer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 22rpx;
  padding-bottom: 28rpx;
}

.agreement-footer-column {
  display: flex;
  flex-direction: column;
  gap: 12rpx;

  .footer-heading {
    font-size: 24rpx;
    font-weight: 700;
    color: #2f241a;
  }

  text {
    font-size: 22rpx;
    color: #7c6b59;
    line-height: 1.7;
  }
}

.agreement-footer-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16rpx;
  flex-wrap: wrap;
  padding-top: 22rpx;
  border-top: 1rpx solid rgba(32, 23, 18, 0.08);

  text {
    font-size: 22rpx;
    color: #7c6b59;
  }
}

@media screen and (max-width: 768px) {
  .agreement-page {
    padding: 18rpx 18rpx 36rpx;
  }

  .agreement-hero {
    padding: 34rpx 26rpx 28rpx;
    border-radius: 28rpx;
  }

  .agreement-title {
    font-size: 44rpx;
    line-height: 54rpx;
  }

  .agreement-desc {
    font-size: 24rpx;
    line-height: 38rpx;
  }

  .hero-summary {
    grid-template-columns: 1fr;
  }

  .agreement-content {
    padding: 24rpx;
  }

  .agreement-content .artist-summary {
    grid-template-columns: 1fr;
  }

  .agreement-content .summary-item.highlight {
    grid-column: span 1;
  }

  .agreement-content .content-section .section-content {
    font-size: 25rpx;
    line-height: 1.82;
  }

  .agreement-footer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
